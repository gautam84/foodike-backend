package com.example.foodike.order.domain.service

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.order.domain.model.Cart
import com.example.foodike.order.domain.model.CartItem
import com.example.foodike.order.domain.port.CartRepository
import com.example.foodike.order.domain.port.CatalogQueryPort
import java.time.Clock
import java.time.Instant
import java.util.UUID

private const val MAX_QUANTITY = 99

class CartService(
    private val cartRepository: CartRepository,
    private val catalog: CatalogQueryPort,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun getCart(userId: String): Cart =
        cartRepository.findByUser(userId) ?: emptyCart(userId)

    suspend fun addItem(userId: String, menuItemId: String, quantity: Int): Cart {
        val qty = requireQuantity(quantity)
        val item = catalog.findMenuItem(menuItemId)
            ?: throw NotFoundException("Menu item not found")
        if (!item.isAvailable) throw ConflictException("Menu item is not available")

        val current = cartRepository.findByUser(userId) ?: emptyCart(userId)
        // Single-restaurant cart: switching restaurants replaces the cart contents.
        val base = if (current.restaurantId != null && current.restaurantId != item.restaurantId) {
            current.copy(restaurantId = item.restaurantId, items = emptyList())
        } else {
            current.copy(restaurantId = item.restaurantId)
        }

        val existing = base.items.firstOrNull { it.menuItemId == menuItemId }
        val items = if (existing == null) {
            base.items + CartItem(
                id = UUID.randomUUID().toString(),
                menuItemId = item.id,
                name = item.name,
                unitPrice = item.price,
                quantity = qty,
            )
        } else {
            base.items.map {
                if (it.menuItemId == menuItemId) {
                    it.copy(quantity = requireQuantity(it.quantity + qty), unitPrice = item.price)
                } else {
                    it
                }
            }
        }
        return cartRepository.save(base.copy(items = items, updatedAt = Instant.now(clock)))
    }

    suspend fun updateItemQuantity(userId: String, cartItemId: String, quantity: Int): Cart {
        val current = cartRepository.findByUser(userId) ?: throw NotFoundException("Cart is empty")
        current.items.firstOrNull { it.id == cartItemId }
            ?: throw NotFoundException("Cart item not found")
        if (quantity <= 0) return removeItem(userId, cartItemId)
        val qty = requireQuantity(quantity)
        val items = current.items.map { if (it.id == cartItemId) it.copy(quantity = qty) else it }
        return cartRepository.save(current.copy(items = items, updatedAt = Instant.now(clock)))
    }

    suspend fun removeItem(userId: String, cartItemId: String): Cart {
        val current = cartRepository.findByUser(userId) ?: throw NotFoundException("Cart is empty")
        current.items.firstOrNull { it.id == cartItemId }
            ?: throw NotFoundException("Cart item not found")
        val items = current.items.filterNot { it.id == cartItemId }
        val restaurantId = if (items.isEmpty()) null else current.restaurantId
        return cartRepository.save(
            current.copy(items = items, restaurantId = restaurantId, updatedAt = Instant.now(clock)),
        )
    }

    suspend fun clear(userId: String): Cart {
        cartRepository.clear(userId)
        return emptyCart(userId)
    }

    private fun emptyCart(userId: String): Cart =
        Cart(
            id = UUID.randomUUID().toString(),
            userId = userId,
            restaurantId = null,
            items = emptyList(),
            updatedAt = Instant.now(clock),
        )

    private fun requireQuantity(quantity: Int): Int {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw ValidationException("Cart is not valid", mapOf("quantity" to "Must be 1-$MAX_QUANTITY"))
        }
        return quantity
    }
}
