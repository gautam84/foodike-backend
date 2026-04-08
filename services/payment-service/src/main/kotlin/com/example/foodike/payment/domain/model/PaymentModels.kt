package com.example.foodike.payment.domain.model

import com.example.foodike.common.model.Money
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
    val amount: Money,
    val status: PaymentStatus = PaymentStatus.PENDING,
)
