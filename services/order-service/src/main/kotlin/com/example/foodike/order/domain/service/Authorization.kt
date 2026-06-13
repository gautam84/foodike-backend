package com.example.foodike.order.domain.service

import com.example.foodike.auth.UserRole
import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.order.domain.model.Order

/** Identity of the caller performing an order action, derived from the JWT principal. */
data class Actor(
    val userId: String,
    val role: UserRole,
)

/** Roles permitted to advance an order through its fulfilment lifecycle. */
private val STAFF_ROLES = setOf(UserRole.RESTAURANT_OWNER, UserRole.ADMIN, UserRole.DELIVERY)

/** Caller must hold a staff role to change an order's status. */
internal fun Actor.requireStaffRole() {
    if (role !in STAFF_ROLES) {
        throw ForbiddenException("Requires restaurant owner, admin, or delivery role")
    }
}

/** Caller must own [order], or hold a staff role, to read it. */
internal fun Actor.requireCanView(order: Order) {
    if (role in STAFF_ROLES) return
    if (order.userId != userId) {
        throw ForbiddenException("You do not have access to this order")
    }
}
