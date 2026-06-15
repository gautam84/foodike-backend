package com.example.foodike.payment.infrastructure.gateway

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.model.Money
import com.example.foodike.payment.domain.port.GatewayOrder
import com.example.foodike.payment.domain.port.GatewayRefund
import com.example.foodike.payment.domain.port.PaymentGatewayPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Razorpay credentials and the active gateway mode (`live` selects this adapter). */
data class RazorpayConfig(
    val mode: String,
    val keyId: String,
    val keySecret: String,
)

/**
 * Live [PaymentGatewayPort] backed by Razorpay's Orders/Refunds REST API. Uses the JDK HTTP client
 * with HTTP Basic auth (keyId:keySecret); payment authenticity is checked with the documented
 * HMAC-SHA256 signature over `"$orderId|$paymentId"`.
 */
class RazorpayGateway(
    private val config: RazorpayConfig,
    private val baseUrl: String = "https://api.razorpay.com/v1",
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) : PaymentGatewayPort {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun createOrder(amount: Money, receipt: String): GatewayOrder {
        val body = buildJsonObject {
            put("amount", amount.amount)
            put("currency", amount.currency)
            put("receipt", receipt)
        }
        val response = post("$baseUrl/orders", body)
        if (response.statusCode() !in 200..299) {
            throw ConflictException("Razorpay order creation failed (${response.statusCode()})")
        }
        val id = json.parseToJsonElement(response.body()).let { it as JsonObject }["id"]
            ?.jsonPrimitive?.content
            ?: throw ConflictException("Razorpay order response missing id")
        return GatewayOrder(gatewayOrderId = id)
    }

    override fun verifySignature(
        gatewayOrderId: String,
        gatewayPaymentId: String,
        signature: String,
    ): Boolean {
        val expected = hmacSha256Hex("$gatewayOrderId|$gatewayPaymentId", config.keySecret)
        return constantTimeEquals(expected, signature)
    }

    override suspend fun refund(gatewayPaymentId: String, amount: Money): GatewayRefund {
        val body = buildJsonObject { put("amount", amount.amount) }
        val response = post("$baseUrl/payments/$gatewayPaymentId/refund", body)
        if (response.statusCode() !in 200..299) {
            throw ConflictException("Razorpay refund failed (${response.statusCode()})")
        }
        val id = json.parseToJsonElement(response.body()).let { it as JsonObject }["id"]
            ?.jsonPrimitive?.content
            ?: throw ConflictException("Razorpay refund response missing id")
        return GatewayRefund(refundId = id)
    }

    private suspend fun post(url: String, body: JsonObject): HttpResponse<String> {
        val credentials = Base64.getEncoder()
            .encodeToString("${config.keyId}:${config.keySecret}".toByteArray(StandardCharsets.UTF_8))
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Basic $credentials")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
        return withContext(Dispatchers.IO) {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }
    }

    private fun hmacSha256Hex(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
