package com.example.foodike.order.infrastructure.persistence

import com.example.foodike.common.model.Money
import com.example.foodike.order.domain.model.Cart
import com.example.foodike.order.domain.model.CartItem
import com.example.foodike.order.domain.port.CartRepository
import com.example.foodike.persistence.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedCartRepository : CartRepository {
    override suspend fun findByUser(userId: String): Cart? =
        dbQuery { loadCart(userId) }

    override suspend fun save(cart: Cart): Cart =
        dbQuery {
            val existingId = CartsTable
                .selectAll()
                .where { CartsTable.userId eq cart.userId }
                .singleOrNull()
                ?.get(CartsTable.id)
            val cartId = existingId ?: cart.id
            if (existingId == null) {
                CartsTable.insert {
                    it[id] = cartId
                    it[userId] = cart.userId
                    it[restaurantId] = cart.restaurantId
                    it[updatedAt] = cart.updatedAt
                }
            } else {
                CartsTable.update({ CartsTable.id eq cartId }) {
                    it[restaurantId] = cart.restaurantId
                    it[updatedAt] = cart.updatedAt
                }
                CartItemsTable.deleteWhere { CartItemsTable.cartId eq cartId }
            }
            cart.items.forEach { item ->
                CartItemsTable.insert {
                    it[id] = item.id
                    it[CartItemsTable.cartId] = cartId
                    it[menuItemId] = item.menuItemId
                    it[name] = item.name
                    it[unitPriceAmount] = item.unitPrice.amount
                    it[currency] = item.unitPrice.currency
                    it[quantity] = item.quantity
                }
            }
            loadCart(cart.userId) ?: error("Cart vanished after save")
        }

    override suspend fun clear(userId: String): Boolean =
        dbQuery {
            CartsTable.deleteWhere { CartsTable.userId eq userId } > 0
        }

    private fun loadCart(userId: String): Cart? {
        val header = CartsTable
            .selectAll()
            .where { CartsTable.userId eq userId }
            .singleOrNull()
            ?: return null
        val cartId = header[CartsTable.id]
        val items = CartItemsTable
            .selectAll()
            .where { CartItemsTable.cartId eq cartId }
            .map { it.toCartItem() }
        return Cart(
            id = cartId,
            userId = header[CartsTable.userId],
            restaurantId = header[CartsTable.restaurantId],
            items = items,
            updatedAt = header[CartsTable.updatedAt],
        )
    }
}

private fun ResultRow.toCartItem() =
    CartItem(
        id = this[CartItemsTable.id],
        menuItemId = this[CartItemsTable.menuItemId],
        name = this[CartItemsTable.name],
        unitPrice = Money(amount = this[CartItemsTable.unitPriceAmount], currency = this[CartItemsTable.currency]),
        quantity = this[CartItemsTable.quantity],
    )
