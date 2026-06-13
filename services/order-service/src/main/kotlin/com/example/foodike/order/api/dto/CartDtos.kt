package com.example.foodike.order.api.dto

import com.example.foodike.order.domain.model.Cart
import com.example.foodike.order.domain.model.CartItem
import kotlinx.serialization.Serializable

@Serializable
data class AddCartItemRequest(
    val menuItemId: String,
    val quantity: Int = 1,
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int,
)

@Serializable
data class CartItemResponse(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPriceAmount: Int,
    val currency: String,
    val quantity: Int,
    val lineTotalAmount: Int,
) {
    companion object {
        fun from(item: CartItem): CartItemResponse =
            CartItemResponse(
                id = item.id,
                menuItemId = item.menuItemId,
                name = item.name,
                unitPriceAmount = item.unitPrice.amount,
                currency = item.unitPrice.currency,
                quantity = item.quantity,
                lineTotalAmount = item.unitPrice.amount * item.quantity,
            )
    }
}

@Serializable
data class CartResponse(
    val id: String,
    val restaurantId: String? = null,
    val items: List<CartItemResponse>,
    val subtotalAmount: Int,
    val currency: String,
    val updatedAt: String,
) {
    companion object {
        fun from(cart: Cart): CartResponse {
            val currency = cart.items.firstOrNull()?.unitPrice?.currency ?: "INR"
            return CartResponse(
                id = cart.id,
                restaurantId = cart.restaurantId,
                items = cart.items.map(CartItemResponse::from),
                subtotalAmount = cart.items.sumOf { it.unitPrice.amount * it.quantity },
                currency = currency,
                updatedAt = cart.updatedAt.toString(),
            )
        }
    }
}
