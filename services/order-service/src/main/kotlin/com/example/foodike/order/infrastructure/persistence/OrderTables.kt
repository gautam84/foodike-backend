package com.example.foodike.order.infrastructure.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object OrdersTable : Table("orders") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).index()
    val restaurantId = varchar("restaurant_id", 36).index()
    val addressId = varchar("address_id", 36).nullable()
    val subtotalAmount = integer("subtotal_amount")
    val deliveryFeeAmount = integer("delivery_fee_amount")
    val totalAmount = integer("total_amount")
    val currency = varchar("currency", 3).default("INR")
    val status = varchar("status", 20)
    val placedAt = timestamp("placed_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36)
        .references(OrdersTable.id, onDelete = ReferenceOption.CASCADE)
        .index()
    val menuItemId = varchar("menu_item_id", 36)
    val name = varchar("name", 150)
    val unitPriceAmount = integer("unit_price_amount")
    val currency = varchar("currency", 3).default("INR")
    val quantity = integer("quantity")
    val lineTotalAmount = integer("line_total_amount")

    override val primaryKey = PrimaryKey(id)
}
