package com.example.foodike.tracking.domain.service

import com.example.foodike.auth.UserRole
import com.example.foodike.common.exception.ForbiddenException

/** Identity of the caller performing a tracking action, derived from the JWT principal. */
data class Actor(
    val userId: String,
    val role: UserRole,
)

/** Roles permitted to manage a delivery (assign couriers, advance status, read any session). */
private val STAFF_ROLES = setOf(UserRole.RESTAURANT_OWNER, UserRole.ADMIN, UserRole.DELIVERY)

/** Roles permitted to push live courier location pings. */
private val COURIER_ROLES = setOf(UserRole.DELIVERY, UserRole.ADMIN)

/** Caller must hold a staff role to assign a courier or advance a delivery's status. */
internal fun Actor.requireStaffRole() {
    if (role !in STAFF_ROLES) {
        throw ForbiddenException("Requires restaurant owner, admin, or delivery role")
    }
}

/** Caller must be a courier (or admin) to report live location. */
internal fun Actor.requireCourierRole() {
    if (role !in COURIER_ROLES) {
        throw ForbiddenException("Requires delivery or admin role")
    }
}

/** Caller must be the order's customer ([ownerId]), or hold a staff role, to view tracking. */
internal fun Actor.requireCanView(ownerId: String) {
    if (role in STAFF_ROLES) return
    if (ownerId != userId) {
        throw ForbiddenException("You do not have access to this delivery")
    }
}
