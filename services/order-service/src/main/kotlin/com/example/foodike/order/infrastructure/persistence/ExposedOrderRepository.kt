package com.example.foodike.order.infrastructure.persistence

import com.example.foodike.common.model.Money
import com.example.foodike.common.model.PageRequest
import com.example.foodike.order.domain.model.Order
import com.example.foodike.order.domain.model.OrderItem
import com.example.foodike.order.domain.model.OrderStatus
import com.example.foodike.order.domain.port.OrderPage
import com.example.foodike.order.domain.port.OrderRepository
import com.example.foodike.persistence.dbQuery
import java.time.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedOrderRepository : OrderRepository {
    override suspend fun create(order: Order): Order =
        dbQuery {
            OrdersTable.insert { it.fromOrder(order) }
            order.items.forEach { item ->
                OrderItemsTable.insert { it.fromItem(order.id, item) }
            }
            order
        }

    override suspend fun findById(id: String): Order? =
        dbQuery {
            val header = OrdersTable
                .selectAll()
                .where { OrdersTable.id eq id }
                .singleOrNull()
                ?: return@dbQuery null
            header.toOrder(loadItems(id))
        }

    override suspend fun findByUser(userId: String, page: PageRequest): OrderPage =
        dbQuery {
            val total = OrdersTable.selectAll().where { OrdersTable.userId eq userId }.count()
            val offset = ((page.page - 1).coerceAtLeast(0).toLong()) * page.size
            val headers = OrdersTable
                .selectAll()
                .where { OrdersTable.userId eq userId }
                .orderBy(OrdersTable.placedAt to SortOrder.DESC)
                .limit(page.size)
                .offset(offset)
                .toList()
            val orders = headers.map { it.toOrder(loadItems(it[OrdersTable.id])) }
            OrderPage(items = orders, total = total)
        }

    override suspend fun updateStatus(id: String, status: OrderStatus): Order? =
        dbQuery {
            val rows = OrdersTable.update({ OrdersTable.id eq id }) {
                it[OrdersTable.status] = status.name
                it[updatedAt] = Instant.now()
            }
            if (rows == 0) return@dbQuery null
            OrdersTable
                .selectAll()
                .where { OrdersTable.id eq id }
                .single()
                .toOrder(loadItems(id))
        }

    private fun loadItems(orderId: String): List<OrderItem> =
        OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.orderId eq orderId }
            .map { it.toItem() }
}

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromOrder(order: Order) {
    this[OrdersTable.id] = order.id
    this[OrdersTable.userId] = order.userId
    this[OrdersTable.restaurantId] = order.restaurantId
    this[OrdersTable.addressId] = order.addressId
    this[OrdersTable.subtotalAmount] = order.subtotal.amount
    this[OrdersTable.deliveryFeeAmount] = order.deliveryFee.amount
    this[OrdersTable.totalAmount] = order.total.amount
    this[OrdersTable.currency] = order.total.currency
    this[OrdersTable.status] = order.status.name
    this[OrdersTable.placedAt] = order.placedAt
    this[OrdersTable.updatedAt] = order.updatedAt
}

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromItem(orderId: String, item: OrderItem) {
    this[OrderItemsTable.id] = item.id
    this[OrderItemsTable.orderId] = orderId
    this[OrderItemsTable.menuItemId] = item.menuItemId
    this[OrderItemsTable.name] = item.name
    this[OrderItemsTable.unitPriceAmount] = item.unitPrice.amount
    this[OrderItemsTable.currency] = item.unitPrice.currency
    this[OrderItemsTable.quantity] = item.quantity
    this[OrderItemsTable.lineTotalAmount] = item.lineTotal.amount
}

private fun ResultRow.toOrder(items: List<OrderItem>): Order {
    val currency = this[OrdersTable.currency]
    return Order(
        id = this[OrdersTable.id],
        userId = this[OrdersTable.userId],
        restaurantId = this[OrdersTable.restaurantId],
        items = items,
        subtotal = Money(amount = this[OrdersTable.subtotalAmount], currency = currency),
        deliveryFee = Money(amount = this[OrdersTable.deliveryFeeAmount], currency = currency),
        total = Money(amount = this[OrdersTable.totalAmount], currency = currency),
        status = OrderStatus.valueOf(this[OrdersTable.status]),
        addressId = this[OrdersTable.addressId],
        placedAt = this[OrdersTable.placedAt],
        updatedAt = this[OrdersTable.updatedAt],
    )
}

private fun ResultRow.toItem() =
    OrderItem(
        id = this[OrderItemsTable.id],
        menuItemId = this[OrderItemsTable.menuItemId],
        name = this[OrderItemsTable.name],
        unitPrice = Money(amount = this[OrderItemsTable.unitPriceAmount], currency = this[OrderItemsTable.currency]),
        quantity = this[OrderItemsTable.quantity],
        lineTotal = Money(amount = this[OrderItemsTable.lineTotalAmount], currency = this[OrderItemsTable.currency]),
    )
