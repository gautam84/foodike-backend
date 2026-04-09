package com.example.foodike.user.api.dto

import com.example.foodike.user.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val phone: String,
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val otp: String,
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
    val isNewUser: Boolean,
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
