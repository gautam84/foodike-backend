package com.example.foodike.order.domain.service

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.Money
import com.example.foodike.common.model.PageRequest
import com.example.foodike.events.EventBus
import com.example.foodike.events.OrderEvents
import com.example.foodike.order.domain.model.Order
import com.example.foodike.order.domain.model.OrderItem
import com.example.foodike.order.domain.model.OrderStatus
import com.example.foodike.order.domain.port.CartRepository
import com.example.foodike.order.domain.port.CatalogQueryPort
import com.example.foodike.order.domain.port.OrderPage
import com.example.foodike.order.domain.port.OrderRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Permitted forward transitions; states absent as keys (DELIVERED, CANCELLED) are terminal. */
private val ALLOWED_TRANSITIONS: Map<OrderStatus, Set<OrderStatus>> = mapOf(
    OrderStatus.CREATED to setOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
    OrderStatus.CONFIRMED to setOf(OrderStatus.PREPARING, OrderStatus.CANCELLED),
    OrderStatus.PREPARING to setOf(OrderStatus.OUT_FOR_DELIVERY),
    OrderStatus.OUT_FOR_DELIVERY to setOf(OrderStatus.DELIVERED),
)

private val CUSTOMER_CANCELLABLE = setOf(OrderStatus.CREATED, OrderStatus.CONFIRMED)

class OrderService(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val catalog: CatalogQueryPort,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun checkout(userId: String, addressId: String?): Order {
        val cart = cartRepository.findByUser(userId)
        if (cart == null || cart.items.isEmpty() || cart.restaurantId == null) {
            throw ConflictException("Cart is empty")
        }
        val restaurant = catalog.findRestaurant(cart.restaurantId)
            ?: throw NotFoundException("Restaurant not found")
        if (!restaurant.enabled) throw ConflictException("Restaurant is not accepting orders")

        val items = cart.items.map { cartItem ->
            val menuItem = catalog.findMenuItem(cartItem.menuItemId)
                ?: throw ConflictException("Menu item '${cartItem.name}' is no longer available")
            if (!menuItem.isAvailable) {
                throw ConflictException("Menu item '${menuItem.name}' is no longer available")
            }
            OrderItem(
                id = UUID.randomUUID().toString(),
                menuItemId = menuItem.id,
                name = menuItem.name,
                unitPrice = menuItem.price,
                quantity = cartItem.quantity,
                lineTotal = Money(
                    amount = menuItem.price.amount * cartItem.quantity,
                    currency = menuItem.price.currency,
                ),
            )
        }

        val currency = items.first().unitPrice.currency
        val subtotal = Money(amount = items.sumOf { it.lineTotal.amount }, currency = currency)
        if (subtotal.amount < restaurant.minOrder) {
            throw ValidationException(
                "Order below minimum",
                mapOf("subtotal" to "Minimum order is ${restaurant.minOrder}"),
            )
        }
        val deliveryFee = Money(amount = restaurant.deliveryFee, currency = currency)
        val total = Money(amount = subtotal.amount + deliveryFee.amount, currency = currency)

        val now = Instant.now(clock)
        val order = orderRepository.create(
            Order(
                id = UUID.randomUUID().toString(),
                userId = userId,
                restaurantId = restaurant.id,
                items = items,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                total = total,
                status = OrderStatus.CREATED,
                addressId = addressId,
                placedAt = now,
                updatedAt = now,
            ),
        )
        cartRepository.clear(userId)
        eventBus.publish(OrderEvents.orderPlaced(orderId = order.id, userId = userId))
        return order
    }

    suspend fun listOrders(userId: String, page: PageRequest): OrderPage =
        orderRepository.findByUser(userId, normalizePage(page))

    suspend fun getOrder(actor: Actor, orderId: String): Order {
        val order = orderRepository.findById(orderId) ?: throw NotFoundException("Order not found")
        actor.requireCanView(order)
        return order
    }

    suspend fun cancel(userId: String, orderId: String): Order {
        val order = orderRepository.findById(orderId) ?: throw NotFoundException("Order not found")
        if (order.userId != userId) throw ForbiddenException("You do not have access to this order")
        if (order.status !in CUSTOMER_CANCELLABLE) {
            throw ConflictException("Order can no longer be cancelled")
        }
        return orderRepository.updateStatus(orderId, OrderStatus.CANCELLED)
            ?: throw NotFoundException("Order not found")
    }

    suspend fun updateStatus(actor: Actor, orderId: String, target: OrderStatus): Order {
        actor.requireStaffRole()
        val order = orderRepository.findById(orderId) ?: throw NotFoundException("Order not found")
        val allowed = ALLOWED_TRANSITIONS[order.status]
            ?: throw ConflictException("Order is in a terminal state: ${order.status}")
        if (target !in allowed) {
            throw ValidationException(
                "Invalid status transition",
                mapOf("status" to "Cannot move from ${order.status} to $target"),
            )
        }
        return orderRepository.updateStatus(orderId, target)
            ?: throw NotFoundException("Order not found")
    }

    private fun normalizePage(page: PageRequest): PageRequest =
        page.copy(
            page = page.page.coerceAtLeast(1),
            size = page.size.coerceIn(1, 100),
        )
}
