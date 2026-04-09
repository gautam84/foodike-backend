package com.example.foodike.user.infrastructure.auth

import com.example.foodike.common.exception.ValidationException
import com.example.foodike.user.domain.model.GoogleProfile
import com.example.foodike.user.domain.port.GoogleIdTokenVerifier
import com.example.foodike.user.domain.port.OtpProvider
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OtpProviderConfig(
    val provider: String = "console",
    val twilioAccountSid: String? = null,
    val twilioAuthToken: String? = null,
    val twilioFromNumber: String? = null,
)

class ConsoleOtpProvider : OtpProvider {
    override suspend fun sendOtp(phone: String, otp: String): Boolean {
        latestOtps[phone] = otp
        println("Foodike OTP for $phone: $otp")
        return true
    }

    companion object {
        private val latestOtps = mutableMapOf<String, String>()

        fun peekLatestOtp(phone: String): String? = latestOtps[phone]
    }
}

class TwilioOtpProvider(
    private val config: OtpProviderConfig,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) : OtpProvider {
    override suspend fun sendOtp(phone: String, otp: String): Boolean {
        val accountSid = config.twilioAccountSid ?: throw ValidationException("TWILIO_ACCOUNT_SID is required")
        val authToken = config.twilioAuthToken ?: throw ValidationException("TWILIO_AUTH_TOKEN is required")
        val fromNumber = config.twilioFromNumber ?: throw ValidationException("TWILIO_FROM_NUMBER is required")

        val formBody =
            listOf(
                "To" to phone,
                "From" to fromNumber,
                "Body" to "Your Foodike OTP is $otp",
            ).joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value)}"
            }

        val authHeader = Base64.getEncoder()
            .encodeToString("$accountSid:$authToken".toByteArray(StandardCharsets.UTF_8))

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"))
            .header("Authorization", "Basic $authHeader")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build()

        val response = withContext(Dispatchers.IO) {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }

        return response.statusCode() in 200..299
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}

class UnsupportedGoogleIdTokenVerifier : GoogleIdTokenVerifier {
    override suspend fun verify(idToken: String): GoogleProfile {
        throw ValidationException("Google sign-in is not wired yet for this environment")
    }
}
