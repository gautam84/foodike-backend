package com.example.foodike.notification.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationChannel {
    PUSH,
    SMS,
    EMAIL,
}

@Serializable
data class Notification(
    val id: String,
    val channel: NotificationChannel,
    val recipient: String,
    val content: String,
)
