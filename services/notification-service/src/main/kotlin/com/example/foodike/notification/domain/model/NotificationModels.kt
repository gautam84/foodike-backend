package com.example.foodike.notification.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationChannel {
    PUSH,
    SMS,
    EMAIL,
}

@Serializable
enum class NotificationType {
    WELCOME,
    ORDER_PLACED,
    GENERIC,
}

@Serializable
enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
}

data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val channel: NotificationChannel,
    val title: String,
    val body: String,
    val recipient: String? = null,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val read: Boolean = false,
    val createdAt: Instant,
    val sentAt: Instant? = null,
)
