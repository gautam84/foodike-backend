package com.example.foodike.restaurant.domain.service

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.Money
import com.example.foodike.restaurant.domain.model.MenuCategory
import com.example.foodike.restaurant.domain.model.MenuItem
import com.example.foodike.restaurant.domain.port.MenuRepository
import com.example.foodike.restaurant.domain.port.RestaurantRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class CreateCategoryInput(
    val name: String,
    val sortOrder: Int?,
)

data class UpdateCategoryInput(
    val name: String?,
    val sortOrder: Int?,
)

data class CreateItemInput(
    val name: String,
    val description: String?,
    val priceAmount: Int,
    val currency: String?,
    val isVeg: Boolean?,
    val isAvailable: Boolean?,
    val imageUrl: String?,
)

data class UpdateItemInput(
    val name: String?,
    val description: String?,
    val priceAmount: Int?,
    val currency: String?,
    val isVeg: Boolean?,
    val isAvailable: Boolean?,
    val imageUrl: String?,
)

class MenuService(
    private val menuRepository: MenuRepository,
    private val restaurantRepository: RestaurantRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun listMenu(restaurantId: String): List<MenuCategory> {
        restaurantRepository.findById(restaurantId) ?: throw NotFoundException("Restaurant not found")
        return menuRepository.findCategoriesWithItems(restaurantId)
    }

    suspend fun createCategory(actor: Actor, restaurantId: String, input: CreateCategoryInput): MenuCategory {
        requireManager(actor, restaurantId)
        val name = requireText(input.name, "name", 100)
        val now = Instant.now(clock)
        return menuRepository.createCategory(
            MenuCategory(
                id = UUID.randomUUID().toString(),
                restaurantId = restaurantId,
                name = name,
                sortOrder = input.sortOrder ?: 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateCategory(actor: Actor, categoryId: String, input: UpdateCategoryInput): MenuCategory {
        val current = menuRepository.findCategory(categoryId) ?: throw NotFoundException("Menu category not found")
        requireManager(actor, current.restaurantId)
        val name = input.name?.let { requireText(it, "name", 100) } ?: current.name
        return menuRepository.updateCategory(
            current.copy(
                name = name,
                sortOrder = input.sortOrder ?: current.sortOrder,
                updatedAt = Instant.now(clock),
            ),
        )
    }

    suspend fun deleteCategory(actor: Actor, categoryId: String) {
        val current = menuRepository.findCategory(categoryId) ?: throw NotFoundException("Menu category not found")
        requireManager(actor, current.restaurantId)
        menuRepository.deleteCategory(categoryId)
    }

    suspend fun createItem(actor: Actor, categoryId: String, input: CreateItemInput): MenuItem {
        val category = menuRepository.findCategory(categoryId) ?: throw NotFoundException("Menu category not found")
        requireManager(actor, category.restaurantId)
        val name = requireText(input.name, "name", 150)
        validatePrice(input.priceAmount)
        val now = Instant.now(clock)
        return menuRepository.createItem(
            MenuItem(
                id = UUID.randomUUID().toString(),
                categoryId = categoryId,
                restaurantId = category.restaurantId,
                name = name,
                description = input.description?.trim()?.ifEmpty { null },
                price = Money(amount = input.priceAmount, currency = normalizeCurrency(input.currency)),
                isVeg = input.isVeg ?: false,
                isAvailable = input.isAvailable ?: true,
                imageUrl = input.imageUrl?.trim()?.ifEmpty { null },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateItem(actor: Actor, itemId: String, input: UpdateItemInput): MenuItem {
        val current = menuRepository.findItem(itemId) ?: throw NotFoundException("Menu item not found")
        requireManager(actor, current.restaurantId)
        val name = input.name?.let { requireText(it, "name", 150) } ?: current.name
        input.priceAmount?.let { validatePrice(it) }
        return menuRepository.updateItem(
            current.copy(
                name = name,
                description = if (input.description == null) current.description else input.description.trim().ifEmpty { null },
                price = Money(
                    amount = input.priceAmount ?: current.price.amount,
                    currency = input.currency?.let { normalizeCurrency(it) } ?: current.price.currency,
                ),
                isVeg = input.isVeg ?: current.isVeg,
                isAvailable = input.isAvailable ?: current.isAvailable,
                imageUrl = if (input.imageUrl == null) current.imageUrl else input.imageUrl.trim().ifEmpty { null },
                updatedAt = Instant.now(clock),
            ),
        )
    }

    suspend fun deleteItem(actor: Actor, itemId: String) {
        val current = menuRepository.findItem(itemId) ?: throw NotFoundException("Menu item not found")
        requireManager(actor, current.restaurantId)
        menuRepository.deleteItem(itemId)
    }

    private suspend fun requireManager(actor: Actor, restaurantId: String) {
        val restaurant = restaurantRepository.findById(restaurantId)
            ?: throw NotFoundException("Restaurant not found")
        actor.requireCanManage(restaurant)
    }

    private fun requireText(value: String, field: String, max: Int): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > max) {
            throw ValidationException("Menu is not valid", mapOf(field to "Length must be 1-$max"))
        }
        return trimmed
    }

    private fun validatePrice(amount: Int) {
        if (amount < 0) throw ValidationException("Menu is not valid", mapOf("priceAmount" to "Must be >= 0"))
    }

    private fun normalizeCurrency(currency: String?): String {
        val value = currency?.trim()?.uppercase()?.ifEmpty { null } ?: "INR"
        if (!Regex("^[A-Z]{3}$").matches(value)) {
            throw ValidationException("Menu is not valid", mapOf("currency" to "Must be ISO 4217 (3 letters)"))
        }
        return value
    }
}
