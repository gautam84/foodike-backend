package com.example.foodike.payment.api.dto

import com.example.foodike.payment.domain.model.Payment
import com.example.foodike.payment.domain.model.PaymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class InitiatePaymentRequest(
    val orderId: String,
)

@Serializable
data class VerifyPaymentRequest(
    val gatewayPaymentId: String,
    val signature: String,
)

@Serializable
data class PaymentResponse(
    val id: String,
    val orderId: String,
    val userId: String,
    val amount: Int,
    val currency: String,
    val status: PaymentStatus,
    val gatewayOrderId: String? = null,
    val gatewayPaymentId: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(payment: Payment): PaymentResponse =
            PaymentResponse(
                id = payment.id,
                orderId = payment.orderId,
                userId = payment.userId,
                amount = payment.amount.amount,
                currency = payment.amount.currency,
                status = payment.status,
                gatewayOrderId = payment.gatewayOrderId,
                gatewayPaymentId = payment.gatewayPaymentId,
                createdAt = payment.createdAt.toString(),
                updatedAt = payment.updatedAt.toString(),
            )
    }
}
