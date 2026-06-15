package com.example.foodike.adapters

import com.example.foodike.order.domain.port.OrderRepository
import com.example.foodike.payment.domain.port.OrderQueryPort
import com.example.foodike.payment.domain.port.OrderSnapshot

/**
 * Bridges payment-service's [OrderQueryPort] to order-service's [OrderRepository]. Lives in the app
 * composition root — the only module permitted to depend on both services — so neither service
 * depends on the other. To extract payment-service into its own process, replace this with an HTTP
 * client; payment-service code is untouched.
 */
class PaymentOrderQueryAdapter(
    private val orderRepository: OrderRepository,
) : OrderQueryPort {
    override suspend fun findOrder(orderId: String): OrderSnapshot? =
        orderRepository.findById(orderId)?.let { order ->
            OrderSnapshot(
                id = order.id,
                userId = order.userId,
                status = order.status.name,
                totalAmount = order.total.amount,
                currency = order.total.currency,
            )
        }
}
