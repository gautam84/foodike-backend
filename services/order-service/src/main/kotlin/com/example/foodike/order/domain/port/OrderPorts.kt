package com.example.foodike.order.domain.port

import com.example.foodike.common.model.PageRequest
import com.example.foodike.order.domain.model.Cart
import com.example.foodike.order.domain.model.Order
import com.example.foodike.order.domain.model.OrderStatus

data class OrderPage(
    val items: List<Order>,
    val total: Long,
)

interface OrderRepository {
    suspend fun create(order: Order): Order
    suspend fun findById(id: String): Order?
    suspend fun findByUser(userId: String, page: PageRequest): OrderPage
    suspend fun updateStatus(id: String, status: OrderStatus): Order?
}

interface CartRepository {
    suspend fun findByUser(userId: String): Cart?
    suspend fun save(cart: Cart): Cart
    suspend fun clear(userId: String): Boolean
}
