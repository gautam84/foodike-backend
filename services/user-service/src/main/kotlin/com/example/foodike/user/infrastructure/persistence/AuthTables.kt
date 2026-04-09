package com.example.foodike.user.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val phone = varchar("phone", 15).nullable().uniqueIndex()
    val googleId = varchar("google_id", 100).nullable().uniqueIndex()
    val email = varchar("email", 255).nullable()
    val name = varchar("name", 100).nullable()
    val avatarUrl = text("avatar_url").nullable()
    val role = varchar("role", 20)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index()
    val tokenHash = varchar("token_hash", 255)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
