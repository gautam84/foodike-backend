package com.example.foodike.notification.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.common.model.PaginatedResponse
import com.example.foodike.notification.api.dto.NotificationResponse
import com.example.foodike.notification.domain.service.NotificationService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.math.ceil
import org.koin.ktor.ext.inject

fun Route.registerNotificationRoutes() {
    val notificationService by inject<NotificationService>()

    route("/notifications") {
        get("/health") {
            call.respond(mapOf("service" to "notification-service", "status" to "ready"))
        }

        authenticate("auth-jwt") {
            get {
                call.respond(listNotifications(notificationService))
            }

            get("/{id}") {
                val principal = call.requirePrincipal()
                val id = call.parameters["id"] ?: throw ValidationException("Notification id required")
                call.respond(NotificationResponse.from(notificationService.get(principal.userId, id)))
            }

            post("/{id}/read") {
                val principal = call.requirePrincipal()
                val id = call.parameters["id"] ?: throw ValidationException("Notification id required")
                call.respond(NotificationResponse.from(notificationService.markRead(principal.userId, id)))
            }
        }
    }
}

private suspend fun RoutingContext.listNotifications(
    notificationService: NotificationService,
): PaginatedResponse<NotificationResponse> {
    val principal = call.requirePrincipal()
    val params = call.request.queryParameters
    val page = params["page"]?.toIntOrNull() ?: 1
    val size = params["size"]?.toIntOrNull() ?: 20
    val result = notificationService.list(principal.userId, PageRequest(page = page, size = size))
    val effectiveSize = size.coerceIn(1, 100)
    val totalPages = if (result.total == 0L) 0 else ceil(result.total.toDouble() / effectiveSize).toInt()
    return PaginatedResponse(
        data = result.items.map(NotificationResponse::from),
        page = page.coerceAtLeast(1),
        size = effectiveSize,
        totalPages = totalPages,
    )
}

private fun ApplicationCall.requirePrincipal(): UserPrincipal =
    principal<UserPrincipal>() ?: throw UnauthorizedException()
