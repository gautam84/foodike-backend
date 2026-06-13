package com.example.foodike.adapters

import com.example.foodike.order.domain.port.OrderRepository
import com.example.foodike.tracking.domain.port.OrderQueryPort
import com.example.foodike.tracking.domain.port.OrderSnapshot

/**
 * Bridges tracking-service's [OrderQueryPort] to order-service's [OrderRepository]. Lives in the app
 * composition root — the only module permitted to depend on both services — so neither service
 * depends on the other. To extract tracking-service into its own process, replace this with an HTTP
 * client; tracking-service code is untouched.
 */
class OrderQueryAdapter(
    private val orderRepository: OrderRepository,
) : OrderQueryPort {
    override suspend fun findOrder(orderId: String): OrderSnapshot? =
        orderRepository.findById(orderId)?.let { order ->
            OrderSnapshot(
                id = order.id,
                userId = order.userId,
                status = order.status.name,
            )
        }
}
