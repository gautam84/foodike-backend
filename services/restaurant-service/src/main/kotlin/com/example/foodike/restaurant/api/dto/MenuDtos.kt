package com.example.foodike.restaurant.api.dto

import com.example.foodike.restaurant.domain.model.MenuCategory
import com.example.foodike.restaurant.domain.model.MenuItem
import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val sortOrder: Int? = null,
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val sortOrder: Int? = null,
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val description: String? = null,
    val priceAmount: Int,
    val currency: String? = null,
    val isVeg: Boolean? = null,
    val isAvailable: Boolean? = null,
    val imageUrl: String? = null,
)

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val description: String? = null,
    val priceAmount: Int? = null,
    val currency: String? = null,
    val isVeg: Boolean? = null,
    val isAvailable: Boolean? = null,
    val imageUrl: String? = null,
)

@Serializable
data class MenuItemResponse(
    val id: String,
    val categoryId: String,
    val restaurantId: String,
    val name: String,
    val description: String? = null,
    val priceAmount: Int,
    val currency: String,
    val isVeg: Boolean,
    val isAvailable: Boolean,
    val imageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(item: MenuItem): MenuItemResponse =
            MenuItemResponse(
                id = item.id,
                categoryId = item.categoryId,
                restaurantId = item.restaurantId,
                name = item.name,
                description = item.description,
                priceAmount = item.price.amount,
                currency = item.price.currency,
                isVeg = item.isVeg,
                isAvailable = item.isAvailable,
                imageUrl = item.imageUrl,
                createdAt = item.createdAt.toString(),
                updatedAt = item.updatedAt.toString(),
            )
    }
}

@Serializable
data class MenuCategoryResponse(
    val id: String,
    val restaurantId: String,
    val name: String,
    val sortOrder: Int,
    val items: List<MenuItemResponse>,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(category: MenuCategory): MenuCategoryResponse =
            MenuCategoryResponse(
                id = category.id,
                restaurantId = category.restaurantId,
                name = category.name,
                sortOrder = category.sortOrder,
                items = category.items.map(MenuItemResponse::from),
                createdAt = category.createdAt.toString(),
                updatedAt = category.updatedAt.toString(),
            )
    }
}
