package com.example.foodike.notification.api.dto

import com.example.foodike.notification.domain.model.Notification
import com.example.foodike.notification.domain.model.NotificationChannel
import com.example.foodike.notification.domain.model.NotificationStatus
import com.example.foodike.notification.domain.model.NotificationType
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val channel: NotificationChannel,
    val title: String,
    val body: String,
    val status: NotificationStatus,
    val read: Boolean,
    val createdAt: String,
    val sentAt: String? = null,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                userId = notification.userId,
                type = notification.type,
                channel = notification.channel,
                title = notification.title,
                body = notification.body,
                status = notification.status,
                read = notification.read,
                createdAt = notification.createdAt.toString(),
                sentAt = notification.sentAt?.toString(),
            )
    }
}
