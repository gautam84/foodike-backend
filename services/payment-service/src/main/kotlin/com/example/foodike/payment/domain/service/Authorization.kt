package com.example.foodike.payment.domain.service

import com.example.foodike.auth.UserRole
import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.payment.domain.model.Payment

/** Identity of the caller performing a payment action, derived from the JWT principal. */
data class Actor(
    val userId: String,
    val role: UserRole,
)

/** Roles permitted to administer payments (e.g. issue refunds). */
private val STAFF_ROLES = setOf(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)

/** Caller must hold a staff role to administer a payment. */
internal fun Actor.requireStaffRole() {
    if (role !in STAFF_ROLES) {
        throw ForbiddenException("Requires restaurant owner or admin role")
    }
}

/** Caller must own [payment], or hold a staff role, to read it. */
internal fun Actor.requireCanView(payment: Payment) {
    if (role in STAFF_ROLES) return
    if (payment.userId != userId) {
        throw ForbiddenException("You do not have access to this payment")
    }
}
