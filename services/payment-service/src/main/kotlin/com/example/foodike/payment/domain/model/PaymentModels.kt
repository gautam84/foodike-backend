package com.example.foodike.payment.domain.model

import com.example.foodike.common.model.Money
import com.example.foodike.common.util.InstantSerializer
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentStatus {
    PENDING,
    INITIATED,
    VERIFIED,
    FAILED,
    REFUNDED,
}

@Serializable
data class Payment(
    val id: String,
    val orderId: String,
    val userId: String,
    val amount: Money,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val gatewayOrderId: String? = null,
    val gatewayPaymentId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)
