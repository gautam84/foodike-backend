package com.example.foodike.order.domain.port

import com.example.foodike.common.model.Money

/**
 * Read-only view of catalog data owned by restaurant-service. Order-service owns these DTOs and the
 * interface; the implementing adapter lives in the app composition root (the only module that may
 * depend on both services). Swapping to an HTTP client during microservice extraction touches only
 * the adapter, never order-service.
 */
interface CatalogQueryPort {
    suspend fun findMenuItem(itemId: String): MenuItemSnapshot?
    suspend fun findRestaurant(restaurantId: String): RestaurantSnapshot?
}

data class MenuItemSnapshot(
    val id: String,
    val restaurantId: String,
    val name: String,
    val price: Money,
    val isAvailable: Boolean,
)

data class RestaurantSnapshot(
    val id: String,
    val name: String,
    val deliveryFee: Int,
    val minOrder: Int,
    val enabled: Boolean,
)
