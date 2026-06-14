package com.example.foodike.tracking.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TrackingSessionsTable : Table("tracking_sessions") {
    val orderId = varchar("order_id", 36)
    val customerId = varchar("customer_id", 36).index()
    val courierId = varchar("courier_id", 36).nullable()
    val status = varchar("status", 30)
    val lat = double("lat").nullable()
    val lng = double("lng").nullable()
    val locationUpdatedAt = timestamp("location_updated_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(orderId)
}
