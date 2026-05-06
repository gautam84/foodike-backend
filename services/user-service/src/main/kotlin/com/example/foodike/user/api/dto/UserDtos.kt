package com.example.foodike.user.api.dto

import com.example.foodike.auth.UserRole
import com.example.foodike.user.domain.model.SsoProvider
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: String,
    val phone: String? = null,
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val role: UserRole,
    val ssoProviders: List<SsoProvider>,
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
)
