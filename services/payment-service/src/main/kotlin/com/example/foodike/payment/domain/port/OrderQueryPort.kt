package com.example.foodike.payment.domain.port

/**
 * Read-only view of order data owned by order-service. Payment-service owns this DTO and the
 * interface; the implementing adapter lives in the app composition root (the only module that may
 * depend on both services). Swapping to an HTTP client during microservice extraction touches only
 * the adapter, never payment-service.
 */
interface OrderQueryPort {
    suspend fun findOrder(orderId: String): OrderSnapshot?
}

data class OrderSnapshot(
    val id: String,
    val userId: String,
    val status: String,
    val totalAmount: Int,
    val currency: String,
)
