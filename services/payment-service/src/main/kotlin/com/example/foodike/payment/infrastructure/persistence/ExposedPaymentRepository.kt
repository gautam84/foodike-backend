package com.example.foodike.payment.infrastructure.persistence

import com.example.foodike.common.model.Money
import com.example.foodike.common.model.PageRequest
import com.example.foodike.payment.domain.model.Payment
import com.example.foodike.payment.domain.model.PaymentStatus
import com.example.foodike.payment.domain.port.PaymentPage
import com.example.foodike.payment.domain.port.PaymentRepository
import com.example.foodike.persistence.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update

class ExposedPaymentRepository : PaymentRepository {
    override suspend fun create(payment: Payment): Payment =
        dbQuery {
            PaymentsTable.insert { it.fromPayment(payment) }
            payment
        }

    override suspend fun findById(id: String): Payment? =
        dbQuery {
            PaymentsTable
                .selectAll()
                .where { PaymentsTable.id eq id }
                .singleOrNull()
                ?.toPayment()
        }

    override suspend fun findByOrder(orderId: String): List<Payment> =
        dbQuery {
            PaymentsTable
                .selectAll()
                .where { PaymentsTable.orderId eq orderId }
                .map { it.toPayment() }
        }

    override suspend fun findByUser(userId: String, page: PageRequest): PaymentPage =
        dbQuery {
            val total = PaymentsTable.selectAll().where { PaymentsTable.userId eq userId }.count()
            val offset = ((page.page - 1).coerceAtLeast(0).toLong()) * page.size
            val items = PaymentsTable
                .selectAll()
                .where { PaymentsTable.userId eq userId }
                .orderBy(PaymentsTable.createdAt to SortOrder.DESC)
                .limit(page.size)
                .offset(offset)
                .map { it.toPayment() }
            PaymentPage(items = items, total = total)
        }

    override suspend fun update(payment: Payment): Payment? =
        dbQuery {
            val rows = PaymentsTable.update({ PaymentsTable.id eq payment.id }) { it.fromPayment(payment) }
            if (rows == 0) return@dbQuery null
            payment
        }
}

private fun UpdateBuilder<*>.fromPayment(payment: Payment) {
    this[PaymentsTable.id] = payment.id
    this[PaymentsTable.orderId] = payment.orderId
    this[PaymentsTable.userId] = payment.userId
    this[PaymentsTable.amount] = payment.amount.amount
    this[PaymentsTable.currency] = payment.amount.currency
    this[PaymentsTable.status] = payment.status.name
    this[PaymentsTable.gatewayOrderId] = payment.gatewayOrderId
    this[PaymentsTable.gatewayPaymentId] = payment.gatewayPaymentId
    this[PaymentsTable.createdAt] = payment.createdAt
    this[PaymentsTable.updatedAt] = payment.updatedAt
}

private fun ResultRow.toPayment(): Payment =
    Payment(
        id = this[PaymentsTable.id],
        orderId = this[PaymentsTable.orderId],
        userId = this[PaymentsTable.userId],
        amount = Money(amount = this[PaymentsTable.amount], currency = this[PaymentsTable.currency]),
        status = PaymentStatus.valueOf(this[PaymentsTable.status]),
        gatewayOrderId = this[PaymentsTable.gatewayOrderId],
        gatewayPaymentId = this[PaymentsTable.gatewayPaymentId],
        createdAt = this[PaymentsTable.createdAt],
        updatedAt = this[PaymentsTable.updatedAt],
    )
