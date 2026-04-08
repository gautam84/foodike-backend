package com.example.foodike.order.domain.model

import com.example.foodike.common.model.Money
import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    CREATED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
}

@Serializable
data class Order(
    val id: String,
    val userId: String,
    val restaurantId: String,
    val total: Money,
    val status: OrderStatus = OrderStatus.CREATED,
)
