package com.example.foodike.payment.infrastructure.gateway

import com.example.foodike.common.model.Money
import com.example.foodike.payment.domain.port.GatewayOrder
import com.example.foodike.payment.domain.port.GatewayRefund
import com.example.foodike.payment.domain.port.PaymentGatewayPort
import java.util.UUID

/**
 * Deterministic [PaymentGatewayPort] for local development and tests — no Razorpay credentials
 * required. Fabricates gateway identifiers and treats the sentinel signature [VALID_SIGNATURE] as
 * authentic, so the full initiate → verify → refund flow can be exercised offline.
 */
class StubPaymentGateway : PaymentGatewayPort {
    override suspend fun createOrder(amount: Money, receipt: String): GatewayOrder =
        GatewayOrder(gatewayOrderId = "order_stub_${UUID.randomUUID().toString().replace("-", "").take(14)}")

    override fun verifySignature(
        gatewayOrderId: String,
        gatewayPaymentId: String,
        signature: String,
    ): Boolean = signature == VALID_SIGNATURE

    override suspend fun refund(gatewayPaymentId: String, amount: Money): GatewayRefund =
        GatewayRefund(refundId = "rfnd_stub_${UUID.randomUUID().toString().replace("-", "").take(14)}")

    companion object {
        const val VALID_SIGNATURE = "valid"
    }
}
