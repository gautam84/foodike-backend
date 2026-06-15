package com.example.foodike.payment.domain.port

import com.example.foodike.common.model.Money

/**
 * Abstraction over the external payment processor. Payment-service owns this interface; concrete
 * adapters (Razorpay for production, a deterministic stub for local/test) live in the
 * infrastructure layer and are selected at wiring time. Extracting payment-service into its own
 * process leaves this contract untouched.
 */
interface PaymentGatewayPort {
    /** Creates a gateway order the client can pay against, returning its identifier. */
    suspend fun createOrder(amount: Money, receipt: String): GatewayOrder

    /** Verifies that [signature] authenticates the [gatewayPaymentId] against [gatewayOrderId]. */
    fun verifySignature(gatewayOrderId: String, gatewayPaymentId: String, signature: String): Boolean

    /** Refunds a previously captured payment in full. */
    suspend fun refund(gatewayPaymentId: String, amount: Money): GatewayRefund
}

data class GatewayOrder(
    val gatewayOrderId: String,
)

data class GatewayRefund(
    val refundId: String,
)
