package com.example.foodike.payment.domain.port

import com.example.foodike.common.model.PageRequest
import com.example.foodike.payment.domain.model.Payment

data class PaymentPage(
    val items: List<Payment>,
    val total: Long,
)

interface PaymentRepository {
    suspend fun create(payment: Payment): Payment
    suspend fun findById(id: String): Payment?
    suspend fun findByOrder(orderId: String): List<Payment>
    suspend fun findByUser(userId: String, page: PageRequest): PaymentPage
    suspend fun update(payment: Payment): Payment?
}
