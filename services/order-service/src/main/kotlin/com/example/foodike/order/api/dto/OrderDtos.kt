package com.example.foodike.order.api.dto

import com.example.foodike.order.domain.model.Order
import com.example.foodike.order.domain.model.OrderItem
import com.example.foodike.order.domain.model.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest(
    val addressId: String? = null,
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: OrderStatus,
)

@Serializable
data class OrderItemResponse(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPriceAmount: Int,
    val currency: String,
    val quantity: Int,
    val lineTotalAmount: Int,
) {
    companion object {
        fun from(item: OrderItem): OrderItemResponse =
            OrderItemResponse(
                id = item.id,
                menuItemId = item.menuItemId,
                name = item.name,
                unitPriceAmount = item.unitPrice.amount,
                currency = item.unitPrice.currency,
                quantity = item.quantity,
                lineTotalAmount = item.lineTotal.amount,
            )
    }
}

@Serializable
data class OrderResponse(
    val id: String,
    val userId: String,
    val restaurantId: String,
    val items: List<OrderItemResponse>,
    val subtotalAmount: Int,
    val deliveryFeeAmount: Int,
    val totalAmount: Int,
    val currency: String,
    val status: OrderStatus,
    val addressId: String? = null,
    val placedAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(order: Order): OrderResponse =
            OrderResponse(
                id = order.id,
                userId = order.userId,
                restaurantId = order.restaurantId,
                items = order.items.map(OrderItemResponse::from),
                subtotalAmount = order.subtotal.amount,
                deliveryFeeAmount = order.deliveryFee.amount,
                totalAmount = order.total.amount,
                currency = order.total.currency,
                status = order.status,
                addressId = order.addressId,
                placedAt = order.placedAt.toString(),
                updatedAt = order.updatedAt.toString(),
            )
    }
}
