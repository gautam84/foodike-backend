package com.example.foodike.restaurant.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ReviewsTable : Table("reviews") {
    val id = varchar("id", 36)
    val restaurantId = varchar("restaurant_id", 36)
        .references(RestaurantsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val userId = varchar("user_id", 36).index()
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_reviews_restaurant_user", restaurantId, userId)
    }
}
