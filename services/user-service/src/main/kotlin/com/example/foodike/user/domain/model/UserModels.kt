package com.example.foodike.user.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val phone: String,
    val fullName: String? = null,
)

@Serializable
data class Address(
    val id: String,
    val label: String,
    val addressLine: String,
)
