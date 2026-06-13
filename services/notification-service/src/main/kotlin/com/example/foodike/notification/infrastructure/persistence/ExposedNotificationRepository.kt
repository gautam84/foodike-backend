package com.example.foodike.notification.infrastructure.persistence

import com.example.foodike.common.model.PageRequest
import com.example.foodike.notification.domain.model.Notification
import com.example.foodike.notification.domain.model.NotificationChannel
import com.example.foodike.notification.domain.model.NotificationStatus
import com.example.foodike.notification.domain.model.NotificationType
import com.example.foodike.notification.domain.port.NotificationPage
import com.example.foodike.notification.domain.port.NotificationRepository
import com.example.foodike.persistence.dbQuery
import java.time.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedNotificationRepository : NotificationRepository {
    override suspend fun create(notification: Notification): Notification =
        dbQuery {
            NotificationsTable.insert { it.fromNotification(notification) }
            notification
        }

    override suspend fun findById(id: String): Notification? =
        dbQuery {
            NotificationsTable
                .selectAll()
                .where { NotificationsTable.id eq id }
                .singleOrNull()
                ?.toNotification()
        }

    override suspend fun findByUser(userId: String, page: PageRequest): NotificationPage =
        dbQuery {
            val total = NotificationsTable.selectAll().where { NotificationsTable.userId eq userId }.count()
            val offset = ((page.page - 1).coerceAtLeast(0).toLong()) * page.size
            val items = NotificationsTable
                .selectAll()
                .where { NotificationsTable.userId eq userId }
                .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
                .limit(page.size)
                .offset(offset)
                .map { it.toNotification() }
            NotificationPage(items = items, total = total)
        }

    override suspend fun markRead(userId: String, id: String): Notification? =
        dbQuery {
            val rows = NotificationsTable.update(
                { (NotificationsTable.id eq id) and (NotificationsTable.userId eq userId) },
            ) {
                it[read] = true
            }
            if (rows == 0) return@dbQuery null
            NotificationsTable.selectAll().where { NotificationsTable.id eq id }.single().toNotification()
        }

    override suspend fun updateStatus(id: String, status: NotificationStatus, sentAt: Instant?): Notification? =
        dbQuery {
            val rows = NotificationsTable.update({ NotificationsTable.id eq id }) {
                it[NotificationsTable.status] = status.name
                it[NotificationsTable.sentAt] = sentAt
            }
            if (rows == 0) return@dbQuery null
            NotificationsTable.selectAll().where { NotificationsTable.id eq id }.single().toNotification()
        }
}

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromNotification(notification: Notification) {
    this[NotificationsTable.id] = notification.id
    this[NotificationsTable.userId] = notification.userId
    this[NotificationsTable.type] = notification.type.name
    this[NotificationsTable.channel] = notification.channel.name
    this[NotificationsTable.title] = notification.title
    this[NotificationsTable.body] = notification.body
    this[NotificationsTable.recipient] = notification.recipient
    this[NotificationsTable.status] = notification.status.name
    this[NotificationsTable.read] = notification.read
    this[NotificationsTable.createdAt] = notification.createdAt
    this[NotificationsTable.sentAt] = notification.sentAt
}

private fun ResultRow.toNotification(): Notification =
    Notification(
        id = this[NotificationsTable.id],
        userId = this[NotificationsTable.userId],
        type = NotificationType.valueOf(this[NotificationsTable.type]),
        channel = NotificationChannel.valueOf(this[NotificationsTable.channel]),
        title = this[NotificationsTable.title],
        body = this[NotificationsTable.body],
        recipient = this[NotificationsTable.recipient],
        status = NotificationStatus.valueOf(this[NotificationsTable.status]),
        read = this[NotificationsTable.read],
        createdAt = this[NotificationsTable.createdAt],
        sentAt = this[NotificationsTable.sentAt],
    )
