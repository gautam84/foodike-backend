package com.example.foodike.user.infrastructure.auth

import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.user.domain.model.SsoProfile
import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.port.SsoIdTokenVerifier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class StubSsoIdTokenVerifier(
    private val provider: SsoProvider,
) : SsoIdTokenVerifier {
    override suspend fun verify(idToken: String): SsoProfile {
        val obj: JsonObject = try {
            Json.parseToJsonElement(idToken).jsonObject
        } catch (_: Exception) {
            throw UnauthorizedException("Stub SSO token must be JSON")
        }
        val subject = obj["subject"]?.jsonPrimitive?.contentOrNull
            ?: throw UnauthorizedException("Stub SSO token missing 'subject'")
        return SsoProfile(
            provider = provider,
            subject = subject,
            email = obj["email"]?.jsonPrimitive?.contentOrNull,
            name = obj["name"]?.jsonPrimitive?.contentOrNull,
            picture = obj["picture"]?.jsonPrimitive?.contentOrNull,
        )
    }
}
