package com.example.foodike.order.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.common.model.PaginatedResponse
import com.example.foodike.order.api.dto.AddCartItemRequest
import com.example.foodike.order.api.dto.CartResponse
import com.example.foodike.order.api.dto.CheckoutRequest
import com.example.foodike.order.api.dto.OrderResponse
import com.example.foodike.order.api.dto.UpdateCartItemRequest
import com.example.foodike.order.api.dto.UpdateOrderStatusRequest
import com.example.foodike.order.domain.service.Actor
import com.example.foodike.order.domain.service.CartService
import com.example.foodike.order.domain.service.OrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.math.ceil
import org.koin.ktor.ext.inject

fun Route.registerOrderRoutes() {
    val cartService by inject<CartService>()
    val orderService by inject<OrderService>()

    authenticate("auth-jwt") {
        route("/cart") {
            get {
                val principal = call.requirePrincipal()
                call.respond(CartResponse.from(cartService.getCart(principal.userId)))
            }

            delete {
                val principal = call.requirePrincipal()
                call.respond(CartResponse.from(cartService.clear(principal.userId)))
            }

            post("/items") {
                val principal = call.requirePrincipal()
                val request = call.receive<AddCartItemRequest>()
                val cart = cartService.addItem(principal.userId, request.menuItemId, request.quantity)
                call.respond(HttpStatusCode.Created, CartResponse.from(cart))
            }

            patch("/items/{itemId}") {
                val principal = call.requirePrincipal()
                val itemId = call.parameters["itemId"] ?: throw ValidationException("Cart item id required")
                val request = call.receive<UpdateCartItemRequest>()
                val cart = cartService.updateItemQuantity(principal.userId, itemId, request.quantity)
                call.respond(CartResponse.from(cart))
            }

            delete("/items/{itemId}") {
                val principal = call.requirePrincipal()
                val itemId = call.parameters["itemId"] ?: throw ValidationException("Cart item id required")
                call.respond(CartResponse.from(cartService.removeItem(principal.userId, itemId)))
            }
        }

        route("/orders") {
            post {
                val principal = call.requirePrincipal()
                val request = call.receiveOrEmpty()
                val order = orderService.checkout(principal.userId, request.addressId)
                call.respond(HttpStatusCode.Created, OrderResponse.from(order))
            }

            get {
                call.respond(listOrders(orderService))
            }

            get("/{id}") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Order id required")
                call.respond(OrderResponse.from(orderService.getOrder(actor, id)))
            }

            post("/{id}/cancel") {
                val principal = call.requirePrincipal()
                val id = call.parameters["id"] ?: throw ValidationException("Order id required")
                call.respond(OrderResponse.from(orderService.cancel(principal.userId, id)))
            }

            patch("/{id}/status") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Order id required")
                val request = call.receive<UpdateOrderStatusRequest>()
                call.respond(OrderResponse.from(orderService.updateStatus(actor, id, request.status)))
            }
        }
    }
}

private suspend fun RoutingContext.listOrders(orderService: OrderService): PaginatedResponse<OrderResponse> {
    val principal = call.requirePrincipal()
    val params = call.request.queryParameters
    val page = params["page"]?.toIntOrNull() ?: 1
    val size = params["size"]?.toIntOrNull() ?: 20
    val result = orderService.listOrders(principal.userId, PageRequest(page = page, size = size))
    val effectiveSize = size.coerceIn(1, 100)
    val totalPages = if (result.total == 0L) 0 else ceil(result.total.toDouble() / effectiveSize).toInt()
    return PaginatedResponse(
        data = result.items.map(OrderResponse::from),
        page = page.coerceAtLeast(1),
        size = effectiveSize,
        totalPages = totalPages,
    )
}

private suspend fun ApplicationCall.receiveOrEmpty(): CheckoutRequest =
    try {
        receive<CheckoutRequest>()
    } catch (_: Exception) {
        CheckoutRequest()
    }

private fun ApplicationCall.requirePrincipal(): UserPrincipal =
    principal<UserPrincipal>() ?: throw UnauthorizedException()

private fun ApplicationCall.actor(): Actor {
    val principal = requirePrincipal()
    return Actor(userId = principal.userId, role = principal.role)
}
