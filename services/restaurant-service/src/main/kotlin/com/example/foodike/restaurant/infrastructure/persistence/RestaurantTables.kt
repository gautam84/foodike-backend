package com.example.foodike.restaurant.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object RestaurantsTable : Table("restaurants") {
    val id = varchar("id", 36)
    val ownerId = varchar("owner_id", 36).index()
    val name = varchar("name", 150)
    val description = text("description").nullable()
    val cuisines = varchar("cuisines", 500).default("")
    val phone = varchar("phone", 20).nullable()
    val line1 = varchar("line1", 255)
    val city = varchar("city", 100)
    val state = varchar("state", 100)
    val postalCode = varchar("postal_code", 20)
    val country = varchar("country", 2)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val imageUrl = text("image_url").nullable()
    val rating = double("rating").default(0.0)
    val ratingCount = integer("rating_count").default(0)
    val deliveryFee = integer("delivery_fee").default(0)
    val minOrder = integer("min_order").default(0)
    val prepTimeMins = integer("prep_time_mins").default(0)
    val enabled = bool("enabled").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RestaurantHoursTable : Table("restaurant_hours") {
    val id = varchar("id", 36)
    val restaurantId = varchar("restaurant_id", 36)
        .references(RestaurantsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val dayOfWeek = integer("day_of_week")
    val opensAt = integer("opens_at")
    val closesAt = integer("closes_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_restaurant_hours_restaurant_day", restaurantId, dayOfWeek)
    }
}
