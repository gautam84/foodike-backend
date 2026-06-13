package com.example.foodike.adapters

import com.example.foodike.order.domain.port.CatalogQueryPort
import com.example.foodike.order.domain.port.MenuItemSnapshot
import com.example.foodike.order.domain.port.RestaurantSnapshot
import com.example.foodike.restaurant.domain.port.MenuRepository
import com.example.foodike.restaurant.domain.port.RestaurantRepository

/**
 * Bridges order-service's [CatalogQueryPort] to restaurant-service's repositories. Lives in the app
 * composition root — the only module permitted to depend on both services — so neither service
 * depends on the other. To extract order-service into its own process, replace this with an HTTP
 * client; order-service code is untouched.
 */
class CatalogQueryAdapter(
    private val menuRepository: MenuRepository,
    private val restaurantRepository: RestaurantRepository,
) : CatalogQueryPort {
    override suspend fun findMenuItem(itemId: String): MenuItemSnapshot? =
        menuRepository.findItem(itemId)?.let { item ->
            MenuItemSnapshot(
                id = item.id,
                restaurantId = item.restaurantId,
                name = item.name,
                price = item.price,
                isAvailable = item.isAvailable,
            )
        }

    override suspend fun findRestaurant(restaurantId: String): RestaurantSnapshot? =
        restaurantRepository.findById(restaurantId)?.let { restaurant ->
            RestaurantSnapshot(
                id = restaurant.id,
                name = restaurant.name,
                deliveryFee = restaurant.deliveryFee,
                minOrder = restaurant.minOrder,
                enabled = restaurant.enabled,
            )
        }
}
