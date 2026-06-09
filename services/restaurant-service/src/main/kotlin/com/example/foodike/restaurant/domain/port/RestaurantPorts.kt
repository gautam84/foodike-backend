package com.example.foodike.restaurant.domain.port

import com.example.foodike.common.model.PageRequest
import com.example.foodike.restaurant.domain.model.MenuCategory
import com.example.foodike.restaurant.domain.model.MenuItem
import com.example.foodike.restaurant.domain.model.Restaurant
import com.example.foodike.restaurant.domain.model.RestaurantHour
import com.example.foodike.restaurant.domain.model.Review

data class RestaurantFilter(
    val query: String? = null,
    val cuisine: String? = null,
    val city: String? = null,
    val enabled: Boolean? = null,
)

data class RestaurantPage(
    val items: List<Restaurant>,
    val total: Long,
)

interface RestaurantRepository {
    suspend fun create(restaurant: Restaurant): Restaurant

    suspend fun update(restaurant: Restaurant): Restaurant

    suspend fun findById(id: String): Restaurant?

    suspend fun delete(id: String): Boolean

    suspend fun search(filter: RestaurantFilter, page: PageRequest): RestaurantPage

    suspend fun replaceHours(restaurantId: String, hours: List<RestaurantHour>): List<RestaurantHour>

    suspend fun findHours(restaurantId: String): List<RestaurantHour>

    suspend fun recomputeRating(restaurantId: String): Restaurant?
}

interface MenuRepository {
    suspend fun createCategory(category: MenuCategory): MenuCategory

    suspend fun updateCategory(category: MenuCategory): MenuCategory

    suspend fun findCategory(id: String): MenuCategory?

    suspend fun deleteCategory(id: String): Boolean

    suspend fun findCategoriesWithItems(restaurantId: String): List<MenuCategory>

    suspend fun createItem(item: MenuItem): MenuItem

    suspend fun updateItem(item: MenuItem): MenuItem

    suspend fun findItem(id: String): MenuItem?

    suspend fun deleteItem(id: String): Boolean
}

interface ReviewRepository {
    suspend fun upsert(review: Review): Review

    suspend fun findByRestaurant(restaurantId: String): List<Review>

    suspend fun findByUser(restaurantId: String, userId: String): Review?

    suspend fun delete(restaurantId: String, userId: String): Boolean
}
