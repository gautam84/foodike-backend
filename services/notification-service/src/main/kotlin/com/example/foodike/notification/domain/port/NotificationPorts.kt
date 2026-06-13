package com.example.foodike.notification.domain.port

import com.example.foodike.common.model.PageRequest
import com.example.foodike.notification.domain.model.Notification
import com.example.foodike.notification.domain.model.NotificationStatus
import java.time.Instant

data class NotificationPage(
    val items: List<Notification>,
    val total: Long,
)

interface NotificationRepository {
    suspend fun create(notification: Notification): Notification
    suspend fun findById(id: String): Notification?
    suspend fun findByUser(userId: String, page: PageRequest): NotificationPage
    suspend fun markRead(userId: String, id: String): Notification?
    suspend fun updateStatus(id: String, status: NotificationStatus, sentAt: Instant?): Notification?
}

/** Channel-dispatch port. Implementations deliver a notification over its channel. */
interface NotificationSender {
    suspend fun send(notification: Notification): Boolean
}
