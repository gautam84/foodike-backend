package com.example.foodike.user.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object AddressesTable : Table("addresses") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
        .references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val label = varchar("label", 50)
    val line1 = varchar("line1", 255)
    val line2 = varchar("line2", 255).nullable()
    val city = varchar("city", 100)
    val state = varchar("state", 100)
    val postalCode = varchar("postal_code", 20)
    val country = varchar("country", 2)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
