
package com.example.foodike.integration

import com.example.foodike.user.infrastructure.auth.ConsoleOtpProvider
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ApplicationModuleTest {
    @Test
    fun rootEndpointExposesArchitectureBanner() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("modular-monolith"))
    }

    @Test
    fun authDocsEndpointServesSwaggerUi() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val response = client.get("/auth/docs")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("SwaggerUI", ignoreCase = true))
    }

    @Test
    fun otpAuthFlowIssuesRotatesAndRevokesTokens() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val phone = randomPhone()
        val sendOtpResponse = client.post("/api/v1/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"$phone"}""")
        }

        assertEquals(HttpStatusCode.OK, sendOtpResponse.status)

        val otp = ConsoleOtpProvider.peekLatestOtp(phone)
        checkNotNull(otp)

        val verifyResponse = client.post("/api/v1/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"$phone","otp":"$otp"}""")
        }

        assertEquals(HttpStatusCode.OK, verifyResponse.status)
        val verifyBody = verifyResponse.bodyAsText()
        val accessToken = extractJsonValue(verifyBody, "accessToken")
        val refreshToken = extractJsonValue(verifyBody, "refreshToken")

        val meResponse = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, meResponse.status)
        assertTrue(meResponse.bodyAsText().contains(phone))

        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshBody = refreshResponse.bodyAsText()
        val rotatedRefreshToken = extractJsonValue(refreshBody, "refreshToken")
        assertNotEquals(refreshToken, rotatedRefreshToken, "Refresh token should rotate")

        val replayedRefreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, replayedRefreshResponse.status)

        val logoutResponse = client.post("/api/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${extractJsonValue(refreshBody, "accessToken")}")
            setBody("""{"refreshToken":"$rotatedRefreshToken"}""")
        }

        assertEquals(HttpStatusCode.OK, logoutResponse.status)
    }

    private fun extractJsonValue(json: String, key: String): String =
        Regex(""""$key"\s*:\s*"([^"]+)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?: error("Missing key '$key' in response: $json")

    private fun randomPhone(): String =
        "+91" + (1_000_000_000L + Random.nextLong(0, 8_999_999_999L)).toString().take(10)
}
