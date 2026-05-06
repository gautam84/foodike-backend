package com.example.foodike.user.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class SsoProvider {
    GOOGLE,
    APPLE,
}

data class SsoProfile(
    val provider: SsoProvider,
    val subject: String,
    val email: String?,
    val name: String?,
    val picture: String?,
)

data class SsoIdentity(
    val id: String,
    val userId: String,
    val provider: SsoProvider,
    val subject: String,
    val email: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
