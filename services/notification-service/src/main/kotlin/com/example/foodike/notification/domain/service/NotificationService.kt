package com.example.foodike.notification.domain.service

import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.notification.domain.model.Notification
import com.example.foodike.notification.domain.model.NotificationChannel
import com.example.foodike.notification.domain.model.NotificationStatus
import com.example.foodike.notification.domain.model.NotificationType
import com.example.foodike.notification.domain.port.NotificationPage
import com.example.foodike.notification.domain.port.NotificationRepository
import com.example.foodike.notification.domain.port.NotificationSender
import java.time.Clock
import java.time.Instant
import java.util.UUID

class NotificationService(
    private val repository: NotificationRepository,
    private val sender: NotificationSender,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** Persists a notification as PENDING, dispatches it, then records the SENT/FAILED outcome. */
    suspend fun notify(
        userId: String,
        type: NotificationType,
        channel: NotificationChannel,
        title: String,
        body: String,
        recipient: String? = null,
    ): Notification {
        val created = repository.create(
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = type,
                channel = channel,
                title = title,
                body = body,
                recipient = recipient,
                status = NotificationStatus.PENDING,
                read = false,
                createdAt = Instant.now(clock),
            ),
        )
        val delivered = try {
            sender.send(created)
        } catch (_: Exception) {
            false
        }
        val status = if (delivered) NotificationStatus.SENT else NotificationStatus.FAILED
        val sentAt = if (delivered) Instant.now(clock) else null
        return repository.updateStatus(created.id, status, sentAt) ?: created.copy(status = status, sentAt = sentAt)
    }

    suspend fun list(userId: String, page: PageRequest): NotificationPage =
        repository.findByUser(userId, normalizePage(page))

    suspend fun get(userId: String, id: String): Notification {
        val notification = repository.findById(id) ?: throw NotFoundException("Notification not found")
        if (notification.userId != userId) {
            throw ForbiddenException("You do not have access to this notification")
        }
        return notification
    }

    suspend fun markRead(userId: String, id: String): Notification {
        // Verify ownership first so a foreign id yields 404/403 rather than a silent no-op.
        get(userId, id)
        return repository.markRead(userId, id) ?: throw NotFoundException("Notification not found")
    }

    private fun normalizePage(page: PageRequest): PageRequest =
        page.copy(
            page = page.page.coerceAtLeast(1),
            size = page.size.coerceIn(1, 100),
        )
}
