package com.example.foodike.user.domain.model

import java.time.Instant

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

data class AuthResult(
    val tokens: TokenPair,
    val user: User,
    val isNewUser: Boolean,
)

data class OtpChallenge(
    val phone: String,
    val codeHash: String,
    val expiresAt: Instant,
)

data class OtpRateLimitState(
    val count: Int,
    val expiresAt: Instant,
)

data class RefreshTokenRecord(
    val id: String,
    val userId: String,
    val tokenHash: String,
    val expiresAt: Instant,
    val createdAt: Instant,
)

data class GoogleProfile(
    val subject: String,
    val email: String?,
    val name: String?,
    val picture: String?,
)
