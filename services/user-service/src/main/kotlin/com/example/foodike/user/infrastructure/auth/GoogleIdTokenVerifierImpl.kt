package com.example.foodike.user.infrastructure.auth

import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.user.domain.model.SsoProfile
import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.port.SsoIdTokenVerifier
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier as GoogleLibVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleIdTokenVerifierImpl(
    clientId: String,
) : SsoIdTokenVerifier {
    init {
        if (clientId.isBlank()) {
            throw IllegalStateException(
                "GOOGLE_OAUTH_CLIENT_ID must be set when AUTH_SSO_GOOGLE_MODE=real",
            )
        }
    }

    private val verifier: GoogleLibVerifier =
        GoogleLibVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(clientId))
            .setIssuers(listOf("https://accounts.google.com", "accounts.google.com"))
            .build()

    override suspend fun verify(idToken: String): SsoProfile {
        if (idToken.isBlank()) throw ValidationException("Google ID token is required")

        val token = withContext(Dispatchers.IO) {
            try {
                verifier.verify(idToken)
            } catch (ex: Exception) {
                throw UnauthorizedException("Invalid Google ID token: ${ex.message}")
            }
        } ?: throw UnauthorizedException("Invalid Google ID token")

        val payload = token.payload
        if (payload.emailVerified == false) {
            throw UnauthorizedException("Google email is not verified")
        }

        return SsoProfile(
            provider = SsoProvider.GOOGLE,
            subject = payload.subject,
            email = payload.email,
            name = payload["name"] as? String,
            picture = payload["picture"] as? String,
        )
    }
}
