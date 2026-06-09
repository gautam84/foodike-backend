package com.example.foodike.restaurant.infrastructure.persistence

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.model.Money
import com.example.foodike.persistence.dbQuery
import com.example.foodike.restaurant.domain.model.MenuCategory
import com.example.foodike.restaurant.domain.model.MenuItem
import com.example.foodike.restaurant.domain.port.MenuRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedMenuRepository : MenuRepository {
    override suspend fun createCategory(category: MenuCategory): MenuCategory =
        dbQuery {
            MenuCategoriesTable.insert {
                it[id] = category.id
                it[restaurantId] = category.restaurantId
                it[name] = category.name
                it[sortOrder] = category.sortOrder
                it[createdAt] = category.createdAt
                it[updatedAt] = category.updatedAt
            }
            category
        }

    override suspend fun updateCategory(category: MenuCategory): MenuCategory =
        dbQuery {
            val rows = MenuCategoriesTable.update({ MenuCategoriesTable.id eq category.id }) {
                it[name] = category.name
                it[sortOrder] = category.sortOrder
                it[updatedAt] = category.updatedAt
            }
            if (rows == 0) throw NotFoundException("Menu category not found")
            category
        }

    override suspend fun findCategory(id: String): MenuCategory? =
        dbQuery {
            MenuCategoriesTable
                .selectAll()
                .where { MenuCategoriesTable.id eq id }
                .singleOrNull()
                ?.toCategory()
        }

    override suspend fun deleteCategory(id: String): Boolean =
        dbQuery {
            MenuCategoriesTable.deleteWhere { MenuCategoriesTable.id eq id } > 0
        }

    override suspend fun findCategoriesWithItems(restaurantId: String): List<MenuCategory> =
        dbQuery {
            val categories = MenuCategoriesTable
                .selectAll()
                .where { MenuCategoriesTable.restaurantId eq restaurantId }
                .orderBy(MenuCategoriesTable.sortOrder to SortOrder.ASC, MenuCategoriesTable.name to SortOrder.ASC)
                .map { it.toCategory() }
            val itemsByCategory = MenuItemsTable
                .selectAll()
                .where { MenuItemsTable.restaurantId eq restaurantId }
                .orderBy(MenuItemsTable.name to SortOrder.ASC)
                .map { it.toItem() }
                .groupBy { it.categoryId }
            categories.map { it.copy(items = itemsByCategory[it.id] ?: emptyList()) }
        }

    override suspend fun createItem(item: MenuItem): MenuItem =
        dbQuery {
            MenuItemsTable.insert {
                it[id] = item.id
                it[categoryId] = item.categoryId
                it[restaurantId] = item.restaurantId
                it[name] = item.name
                it[description] = item.description
                it[priceAmount] = item.price.amount
                it[currency] = item.price.currency
                it[isVeg] = item.isVeg
                it[isAvailable] = item.isAvailable
                it[imageUrl] = item.imageUrl
                it[createdAt] = item.createdAt
                it[updatedAt] = item.updatedAt
            }
            item
        }

    override suspend fun updateItem(item: MenuItem): MenuItem =
        dbQuery {
            val rows = MenuItemsTable.update({ MenuItemsTable.id eq item.id }) {
                it[name] = item.name
                it[description] = item.description
                it[priceAmount] = item.price.amount
                it[currency] = item.price.currency
                it[isVeg] = item.isVeg
                it[isAvailable] = item.isAvailable
                it[imageUrl] = item.imageUrl
                it[updatedAt] = item.updatedAt
            }
            if (rows == 0) throw NotFoundException("Menu item not found")
            item
        }

    override suspend fun findItem(id: String): MenuItem? =
        dbQuery {
            MenuItemsTable
                .selectAll()
                .where { MenuItemsTable.id eq id }
                .singleOrNull()
                ?.toItem()
        }

    override suspend fun deleteItem(id: String): Boolean =
        dbQuery {
            MenuItemsTable.deleteWhere { MenuItemsTable.id eq id } > 0
        }
}

private fun ResultRow.toCategory() =
    MenuCategory(
        id = this[MenuCategoriesTable.id],
        restaurantId = this[MenuCategoriesTable.restaurantId],
        name = this[MenuCategoriesTable.name],
        sortOrder = this[MenuCategoriesTable.sortOrder],
        createdAt = this[MenuCategoriesTable.createdAt],
        updatedAt = this[MenuCategoriesTable.updatedAt],
    )

private fun ResultRow.toItem() =
    MenuItem(
        id = this[MenuItemsTable.id],
        categoryId = this[MenuItemsTable.categoryId],
        restaurantId = this[MenuItemsTable.restaurantId],
        name = this[MenuItemsTable.name],
        description = this[MenuItemsTable.description],
        price = Money(amount = this[MenuItemsTable.priceAmount], currency = this[MenuItemsTable.currency]),
        isVeg = this[MenuItemsTable.isVeg],
        isAvailable = this[MenuItemsTable.isAvailable],
        imageUrl = this[MenuItemsTable.imageUrl],
        createdAt = this[MenuItemsTable.createdAt],
        updatedAt = this[MenuItemsTable.updatedAt],
    )
