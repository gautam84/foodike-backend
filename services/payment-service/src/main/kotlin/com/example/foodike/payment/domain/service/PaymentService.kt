package com.example.foodike.payment.domain.service

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.model.Money
import com.example.foodike.common.model.PageRequest
import com.example.foodike.events.EventBus
import com.example.foodike.events.PaymentEvents
import com.example.foodike.payment.domain.model.Payment
import com.example.foodike.payment.domain.model.PaymentStatus
import com.example.foodike.payment.domain.port.OrderQueryPort
import com.example.foodike.payment.domain.port.PaymentGatewayPort
import com.example.foodike.payment.domain.port.PaymentPage
import com.example.foodike.payment.domain.port.PaymentRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orders: OrderQueryPort,
    private val gateway: PaymentGatewayPort,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** Creates a gateway order for the caller's order and records an INITIATED payment. */
    suspend fun initiate(userId: String, orderId: String): Payment {
        val order = orders.findOrder(orderId) ?: throw NotFoundException("Order not found")
        if (order.userId != userId) throw ForbiddenException("You do not have access to this order")
        if (paymentRepository.findByOrder(orderId).any { it.status == PaymentStatus.VERIFIED }) {
            throw ConflictException("Order is already paid")
        }

        val now = Instant.now(clock)
        val amount = Money(amount = order.totalAmount, currency = order.currency)
        val payment = paymentRepository.create(
            Payment(
                id = UUID.randomUUID().toString(),
                orderId = order.id,
                userId = userId,
                amount = amount,
                status = PaymentStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            ),
        )

        val gatewayOrder = gateway.createOrder(amount = amount, receipt = payment.id)
        return paymentRepository.update(
            payment.copy(
                status = PaymentStatus.INITIATED,
                gatewayOrderId = gatewayOrder.gatewayOrderId,
                updatedAt = Instant.now(clock),
            ),
        ) ?: throw NotFoundException("Payment not found")
    }

    /** Verifies the gateway signature, marking the payment VERIFIED or FAILED accordingly. */
    suspend fun verify(
        userId: String,
        paymentId: String,
        gatewayPaymentId: String,
        signature: String,
    ): Payment {
        val payment = paymentRepository.findById(paymentId) ?: throw NotFoundException("Payment not found")
        if (payment.userId != userId) throw ForbiddenException("You do not have access to this payment")
        if (payment.status != PaymentStatus.INITIATED) {
            throw ConflictException("Payment cannot be verified from state: ${payment.status}")
        }
        val gatewayOrderId = payment.gatewayOrderId
            ?: throw ConflictException("Payment has no gateway order")

        val valid = gateway.verifySignature(gatewayOrderId, gatewayPaymentId, signature)
        val updated = paymentRepository.update(
            payment.copy(
                status = if (valid) PaymentStatus.VERIFIED else PaymentStatus.FAILED,
                gatewayPaymentId = gatewayPaymentId,
                updatedAt = Instant.now(clock),
            ),
        ) ?: throw NotFoundException("Payment not found")

        if (valid) {
            eventBus.publish(
                PaymentEvents.paymentVerified(
                    paymentId = updated.id,
                    orderId = updated.orderId,
                    userId = updated.userId,
                ),
            )
        }
        return updated
    }

    suspend fun get(actor: Actor, paymentId: String): Payment {
        val payment = paymentRepository.findById(paymentId) ?: throw NotFoundException("Payment not found")
        actor.requireCanView(payment)
        return payment
    }

    suspend fun list(userId: String, page: PageRequest): PaymentPage =
        paymentRepository.findByUser(userId, normalizePage(page))

    /** Refunds a verified payment; restricted to staff roles. */
    suspend fun refund(actor: Actor, paymentId: String): Payment {
        actor.requireStaffRole()
        val payment = paymentRepository.findById(paymentId) ?: throw NotFoundException("Payment not found")
        if (payment.status != PaymentStatus.VERIFIED) {
            throw ConflictException("Only a verified payment can be refunded")
        }
        val gatewayPaymentId = payment.gatewayPaymentId
            ?: throw ConflictException("Payment has no captured gateway payment")

        gateway.refund(gatewayPaymentId, payment.amount)
        return paymentRepository.update(
            payment.copy(status = PaymentStatus.REFUNDED, updatedAt = Instant.now(clock)),
        ) ?: throw NotFoundException("Payment not found")
    }

    private fun normalizePage(page: PageRequest): PageRequest =
        page.copy(
            page = page.page.coerceAtLeast(1),
            size = page.size.coerceIn(1, 100),
        )
}
