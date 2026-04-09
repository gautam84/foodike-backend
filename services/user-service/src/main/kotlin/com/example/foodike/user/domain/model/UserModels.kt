package com.example.foodike.user.domain.model

import com.example.foodike.auth.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val phone: String? = null,
    val googleId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.CUSTOMER,
)

@Serializable
data class Address(
    val id: String,
    val label: String,
    val addressLine: String,
)
