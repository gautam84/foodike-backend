package com.example.foodike.restaurant.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object MenuCategoriesTable : Table("menu_categories") {
    val id = varchar("id", 36)
    val restaurantId = varchar("restaurant_id", 36)
        .references(RestaurantsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val name = varchar("name", 100)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object MenuItemsTable : Table("menu_items") {
    val id = varchar("id", 36)
    val categoryId = varchar("category_id", 36)
        .references(MenuCategoriesTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val restaurantId = varchar("restaurant_id", 36)
        .references(RestaurantsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val name = varchar("name", 150)
    val description = text("description").nullable()
    val priceAmount = integer("price_amount")
    val currency = varchar("currency", 3).default("INR")
    val isVeg = bool("is_veg").default(false)
    val isAvailable = bool("is_available").default(true)
    val imageUrl = text("image_url").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
