package com.example.foodike.integration

import com.example.foodike.auth.JwtConfig
import com.example.foodike.auth.JwtService
import com.example.foodike.auth.UserPrincipal
import com.example.foodike.auth.UserRole
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentTest {

    @Test
    fun customerInitiatesVerifiesAndStaffRefundsPayment() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val otherCustomerToken = mintToken("other-${Random.nextLong()}", UserRole.CUSTOMER)
        val adminToken = mintToken("admin-${Random.nextLong()}", UserRole.ADMIN)

        val itemId = seedRestaurantWithItem(ownerToken, deliveryFee = 5000, minOrder = 0, price = 50000)
        val orderId = placeOrder(customerToken, itemId)

        // Initiate: a gateway order is created and the payment moves to INITIATED.
        val initiate = client.post("/api/v1/payments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"orderId":"$orderId"}""")
        }
        assertEquals(HttpStatusCode.Created, initiate.status, initiate.bodyAsText())
        val initiateBody = initiate.bodyAsText()
        val paymentId = extractJsonValue(initiateBody, "id")
        assertTrue(initiateBody.contains("\"INITIATED\""), "payment should be INITIATED: $initiateBody")
        assertTrue(initiateBody.contains("\"gatewayOrderId\""), "gateway order id expected: $initiateBody")
        assertEquals(55000.0, numberField(initiateBody, "amount"), "price + deliveryFee expected: $initiateBody")

        // A different customer cannot read this payment.
        val forbiddenRead = client.get("/api/v1/payments/$paymentId") {
            header(HttpHeaders.Authorization, "Bearer $otherCustomerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenRead.status)

        // Verify with the stub gateway's sentinel signature → VERIFIED.
        val verify = client.post("/api/v1/payments/$paymentId/verify") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"gatewayPaymentId":"pay_test","signature":"valid"}""")
        }
        assertEquals(HttpStatusCode.OK, verify.status, verify.bodyAsText())
        assertTrue(verify.bodyAsText().contains("\"VERIFIED\""), "payment should be VERIFIED: ${verify.bodyAsText()}")

        // The payment appears in the customer's list.
        val list = client.get("/api/v1/payments") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }.bodyAsText()
        assertTrue(list.contains(paymentId), "payment should appear in customer's list: $list")

        // Initiating again for an already-paid order conflicts.
        val duplicate = client.post("/api/v1/payments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"orderId":"$orderId"}""")
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status, duplicate.bodyAsText())

        // A customer cannot refund; an admin can.
        val forbiddenRefund = client.post("/api/v1/payments/$paymentId/refund") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenRefund.status)

        val refund = client.post("/api/v1/payments/$paymentId/refund") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, refund.status, refund.bodyAsText())
        assertTrue(refund.bodyAsText().contains("\"REFUNDED\""), "payment should be REFUNDED: ${refund.bodyAsText()}")
    }

    @Test
    fun verifyWithBadSignatureFailsPayment() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val itemId = seedRestaurantWithItem(ownerToken, deliveryFee = 0, minOrder = 0, price = 12000)
        val orderId = placeOrder(customerToken, itemId)

        val initiate = client.post("/api/v1/payments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"orderId":"$orderId"}""")
        }
        val paymentId = extractJsonValue(initiate.bodyAsText(), "id")

        val verify = client.post("/api/v1/payments/$paymentId/verify") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"gatewayPaymentId":"pay_test","signature":"tampered"}""")
        }
        assertEquals(HttpStatusCode.OK, verify.status, verify.bodyAsText())
        assertTrue(verify.bodyAsText().contains("\"FAILED\""), "bad signature should FAIL: ${verify.bodyAsText()}")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.seedRestaurantWithItem(
        ownerToken: String,
        deliveryFee: Int,
        minOrder: Int,
        price: Int,
    ): String {
        val name = "Eatery ${Random.nextInt(1_000_000)}"
        val createResponse = client.post("/api/v1/restaurants") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody(
                """
                {"name":"$name","line1":"1 Road","city":"Bengaluru","state":"Karnataka",
                 "postalCode":"560001","country":"IN","deliveryFee":$deliveryFee,"minOrder":$minOrder}
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status, createResponse.bodyAsText())
        val restaurantId = extractJsonValue(createResponse.bodyAsText(), "id")

        val categoryResponse = client.post("/api/v1/restaurants/$restaurantId/categories") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"name":"Mains","sortOrder":1}""")
        }
        val categoryId = extractJsonValue(categoryResponse.bodyAsText(), "id")

        val itemResponse = client.post("/api/v1/categories/$categoryId/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"name":"Dish ${Random.nextInt(1_000_000)}","priceAmount":$price}""")
        }
        assertEquals(HttpStatusCode.Created, itemResponse.status, itemResponse.bodyAsText())
        return extractJsonValue(itemResponse.bodyAsText(), "id")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.placeOrder(
        customerToken: String,
        itemId: String,
    ): String {
        client.post("/api/v1/cart/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"menuItemId":"$itemId","quantity":1}""")
        }
        val checkout = client.post("/api/v1/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{}""")
        }
        assertEquals(HttpStatusCode.Created, checkout.status, checkout.bodyAsText())
        return extractJsonValue(checkout.bodyAsText(), "id")
    }

    private fun mintToken(userId: String, role: UserRole): String {
        val jwtService = JwtService(
            JwtConfig(
                secret = "local-dev-secret",
                issuer = "foodike",
                audience = "foodike-clients",
            ),
        )
        return jwtService.generateAccessToken(UserPrincipal(userId = userId, role = role))
    }

    private fun extractJsonValue(json: String, key: String): String =
        Regex(""""$key"\s*:\s*"([^"]+)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?: error("Missing key '$key' in response: $json")

    private fun numberField(json: String, key: String): Double =
        Regex("\"$key\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.toDouble()
            ?: error("Missing numeric key '$key' in response: $json")
}
