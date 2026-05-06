package com.example.foodike.user.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object SsoIdentitiesTable : Table("sso_identities") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
        .references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val provider = varchar("provider", 20)
    val subject = varchar("subject", 255)
    val email = varchar("email", 255).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_sso_identities_provider_subject", provider, subject)
        index("ix_sso_identities_user_provider", false, userId, provider)
    }
}
