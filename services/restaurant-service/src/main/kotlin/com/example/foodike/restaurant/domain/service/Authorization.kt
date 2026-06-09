package com.example.foodike.restaurant.domain.service

import com.example.foodike.auth.UserRole
import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.restaurant.domain.model.Restaurant

/** Identity of the caller performing a write, derived from the JWT principal. */
data class Actor(
    val userId: String,
    val role: UserRole,
)

/** Roles permitted to create and manage restaurants/menus. */
private val MANAGER_ROLES = setOf(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)

/** Caller must hold a manager role to create a restaurant. */
internal fun Actor.requireManagerRole() {
    if (role !in MANAGER_ROLES) {
        throw ForbiddenException("Requires restaurant owner or admin role")
    }
}

/** Caller must be the owner of [restaurant] (or an admin) to mutate it. */
internal fun Actor.requireCanManage(restaurant: Restaurant) {
    requireManagerRole()
    if (role != UserRole.ADMIN && restaurant.ownerId != userId) {
        throw ForbiddenException("You do not manage this restaurant")
    }
}
