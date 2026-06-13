package com.example.foodike.order.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object CartsTable : Table("carts") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val restaurantId = varchar("restaurant_id", 36).nullable()
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_carts_user", userId)
    }
}

object CartItemsTable : Table("cart_items") {
    val id = varchar("id", 36)
    val cartId = varchar("cart_id", 36)
        .references(CartsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val menuItemId = varchar("menu_item_id", 36)
    val name = varchar("name", 150)
    val unitPriceAmount = integer("unit_price_amount")
    val currency = varchar("currency", 3).default("INR")
    val quantity = integer("quantity")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_cart_items_cart_menu_item", cartId, menuItemId)
    }
}
