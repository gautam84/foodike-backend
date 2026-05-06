package com.example.foodike.integration

import com.example.foodike.user.infrastructure.auth.ConsoleOtpProvider
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserProfileTest {

    @Test
    fun googleSsoIssuesTokensAndPersistsIdentity() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val subject = "google-sub-${Random.nextLong()}"
        val email = "user${Random.nextInt(100000)}@example.com"
        val token = """{"subject":"$subject","email":"$email","name":"Alice","picture":"https://x/y.png"}"""

        val firstResponse = client.post("/api/v1/auth/sso/google") {
            contentType(ContentType.Application.Json)
            setBody("""{"idToken":${jsonString(token)}}""")
        }
        assertEquals(HttpStatusCode.OK, firstResponse.status)
        val firstBody = firstResponse.bodyAsText()
        assertTrue(boolField(firstBody, "isNewUser") == true, "expected isNewUser=true in $firstBody")
        val firstUserId = extractJsonValue(firstBody, "id")
        assertNotNull(extractJsonValue(firstBody, "accessToken"))

        val secondResponse = client.post("/api/v1/auth/sso/google") {
            contentType(ContentType.Application.Json)
            setBody("""{"idToken":${jsonString(token)}}""")
        }
        assertEquals(HttpStatusCode.OK, secondResponse.status)
        val secondBody = secondResponse.bodyAsText()
        assertTrue(boolField(secondBody, "isNewUser") == false, "expected isNewUser=false in $secondBody")
        assertEquals(firstUserId, extractJsonValue(secondBody, "id"))

        val aliasResponse = client.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody("""{"idToken":${jsonString(token)}}""")
        }
        assertEquals(HttpStatusCode.OK, aliasResponse.status)
        assertEquals(firstUserId, extractJsonValue(aliasResponse.bodyAsText(), "id"))

        val unknownProviderResponse = client.post("/api/v1/auth/sso/facebook") {
            contentType(ContentType.Application.Json)
            setBody("""{"idToken":"x"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, unknownProviderResponse.status)
    }

    @Test
    fun profileGetReturnsAuthenticatedUserAndPatchUpdatesFields() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val (firstAccess, firstPhone) = signInViaOtp(this)

        val getResponse = client.get("/api/v1/users/me") {
            header(HttpHeaders.Authorization, "Bearer $firstAccess")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains(firstPhone))

        val email = "alice${Random.nextInt(1_000_000)}@example.com"
        val patchResponse = client.patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $firstAccess")
            setBody("""{"name":"Alice","email":"$email"}""")
        }
        assertEquals(HttpStatusCode.OK, patchResponse.status)
        val patchBody = patchResponse.bodyAsText()
        assertEquals("Alice", extractJsonValue(patchBody, "name"))
        assertEquals(email, extractJsonValue(patchBody, "email"))

        val invalidEmailResponse = client.patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $firstAccess")
            setBody("""{"email":"not-an-email"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalidEmailResponse.status)
        assertTrue(invalidEmailResponse.bodyAsText().contains("VALIDATION_ERROR"))

        val (secondAccess, _) = signInViaOtp(this)
        val conflictResponse = client.patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $secondAccess")
            setBody("""{"email":"$email"}""")
        }
        assertEquals(HttpStatusCode.Conflict, conflictResponse.status)
        assertTrue(conflictResponse.bodyAsText().contains("CONFLICT"))
    }

    @Test
    fun addressCrudFlowMaintainsSingleDefault() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val (access, _) = signInViaOtp(this)
        val authHeader = "Bearer $access"

        val emptyList = client.get("/api/v1/users/me/addresses") { header(HttpHeaders.Authorization, authHeader) }
        assertEquals(HttpStatusCode.OK, emptyList.status)
        assertTrue(
            Regex("\"addresses\"\\s*:\\s*\\[\\s*]").containsMatchIn(emptyList.bodyAsText()),
            "expected empty addresses array in ${emptyList.bodyAsText()}",
        )

        val homeBody = """
            {"label":"Home","line1":"1 Park St","city":"Bengaluru","state":"KA","postalCode":"560001","country":"IN"}
        """.trimIndent()
        val createHome = client.post("/api/v1/users/me/addresses") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, authHeader)
            setBody(homeBody)
        }
        assertEquals(HttpStatusCode.Created, createHome.status)
        val homeBodyText = createHome.bodyAsText()
        val homeId = extractJsonValue(homeBodyText, "id")
        assertEquals(true, boolField(homeBodyText, "isDefault"))

        val officeBody = """
            {"label":"Office","line1":"42 MG Rd","city":"Bengaluru","state":"KA","postalCode":"560002","country":"IN"}
        """.trimIndent()
        val createOffice = client.post("/api/v1/users/me/addresses") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, authHeader)
            setBody(officeBody)
        }
        assertEquals(HttpStatusCode.Created, createOffice.status)
        val officeBodyText = createOffice.bodyAsText()
        val officeId = extractJsonValue(officeBodyText, "id")
        assertEquals(false, boolField(officeBodyText, "isDefault"))

        val flipDefault = client.post("/api/v1/users/me/addresses/$officeId/default") {
            header(HttpHeaders.Authorization, authHeader)
        }
        assertEquals(HttpStatusCode.OK, flipDefault.status)
        assertEquals(true, boolField(flipDefault.bodyAsText(), "isDefault"))

        val listAfterFlip = client.get("/api/v1/users/me/addresses") { header(HttpHeaders.Authorization, authHeader) }
        val listBody = listAfterFlip.bodyAsText()
        assertEquals(2, countBoolField(listBody, "isDefault", true) + countBoolField(listBody, "isDefault", false))
        assertEquals(1, countBoolField(listBody, "isDefault", true))

        val patchHome = client.patch("/api/v1/users/me/addresses/$homeId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, authHeader)
            setBody("""{"label":"Home Updated"}""")
        }
        assertEquals(HttpStatusCode.OK, patchHome.status)
        assertEquals("Home Updated", extractJsonValue(patchHome.bodyAsText(), "label"))

        val deleteOffice = client.delete("/api/v1/users/me/addresses/$officeId") {
            header(HttpHeaders.Authorization, authHeader)
        }
        assertEquals(HttpStatusCode.NoContent, deleteOffice.status)

        val listAfterDelete = client.get("/api/v1/users/me/addresses") { header(HttpHeaders.Authorization, authHeader) }
        val finalBody = listAfterDelete.bodyAsText()
        assertEquals(1, Regex("\"id\"\\s*:\\s*\"").findAll(finalBody).count())
        assertEquals(0, countBoolField(finalBody, "isDefault", true))

        val (otherAccess, _) = signInViaOtp(this)
        val crossTenant = client.get("/api/v1/users/me/addresses/$homeId") {
            header(HttpHeaders.Authorization, "Bearer $otherAccess")
        }
        assertEquals(HttpStatusCode.NotFound, crossTenant.status)
    }

    private suspend fun signInViaOtp(app: io.ktor.server.testing.ApplicationTestBuilder): Pair<String, String> {
        val phone = randomPhone()
        val sendResponse = app.client.post("/api/v1/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"$phone"}""")
        }
        assertEquals(HttpStatusCode.OK, sendResponse.status)
        val otp = ConsoleOtpProvider.peekLatestOtp(phone) ?: error("OTP not captured for $phone")
        val verifyResponse = app.client.post("/api/v1/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"$phone","otp":"$otp"}""")
        }
        assertEquals(HttpStatusCode.OK, verifyResponse.status)
        val accessToken = extractJsonValue(verifyResponse.bodyAsText(), "accessToken")
        return accessToken to phone
    }

    private fun randomPhone(): String =
        "+91" + (1_000_000_000L + Random.nextLong(0, 8_999_999_999L)).toString().take(10)

    private fun extractJsonValue(json: String, key: String): String =
        Regex(""""$key"\s*:\s*"([^"]+)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?: error("Missing key '$key' in response: $json")

    private fun boolField(json: String, key: String): Boolean? {
        val match = Regex("\"$key\"\\s*:\\s*(true|false)").find(json) ?: return null
        return match.groupValues[1].toBoolean()
    }

    private fun countBoolField(json: String, key: String, value: Boolean): Int =
        Regex("\"$key\"\\s*:\\s*$value\\b").findAll(json).count()

    private fun jsonString(value: String): String =
        buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
}
