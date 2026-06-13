package com.example.foodike.notification.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object NotificationsTable : Table("notifications") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).index()
    val type = varchar("type", 30)
    val channel = varchar("channel", 10)
    val title = varchar("title", 150)
    val body = varchar("body", 500)
    val recipient = varchar("recipient", 255).nullable()
    val status = varchar("status", 10)
    // "read" is a reserved word in several SQL dialects; store it as is_read.
    val read = bool("is_read").default(false)
    val createdAt = timestamp("created_at")
    val sentAt = timestamp("sent_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
