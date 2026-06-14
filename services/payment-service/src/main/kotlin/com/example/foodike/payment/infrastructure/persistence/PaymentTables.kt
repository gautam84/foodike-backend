package com.example.foodike.payment.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PaymentsTable : Table("payments") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36).index()
    val userId = varchar("user_id", 36).index()
    val amount = integer("amount")
    val currency = varchar("currency", 3).default("INR")
    val status = varchar("status", 20)
    val gatewayOrderId = varchar("gateway_order_id", 64).nullable()
    val gatewayPaymentId = varchar("gateway_payment_id", 64).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
