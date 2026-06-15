package com.example.foodike.payment.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.common.model.PaginatedResponse
import com.example.foodike.payment.api.dto.InitiatePaymentRequest
import com.example.foodike.payment.api.dto.PaymentResponse
import com.example.foodike.payment.api.dto.VerifyPaymentRequest
import com.example.foodike.payment.domain.service.Actor
import com.example.foodike.payment.domain.service.PaymentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.math.ceil
import org.koin.ktor.ext.inject

fun Route.registerPaymentRoutes() {
    val paymentService by inject<PaymentService>()

    route("/payments") {
        get("/health") {
            call.respond(mapOf("service" to "payment-service", "status" to "ready"))
        }

        authenticate("auth-jwt") {
            post {
                val principal = call.requirePrincipal()
                val request = call.receive<InitiatePaymentRequest>()
                val payment = paymentService.initiate(principal.userId, request.orderId)
                call.respond(HttpStatusCode.Created, PaymentResponse.from(payment))
            }

            get {
                call.respond(listPayments(paymentService))
            }

            get("/{id}") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Payment id required")
                call.respond(PaymentResponse.from(paymentService.get(actor, id)))
            }

            post("/{id}/verify") {
                val principal = call.requirePrincipal()
                val id = call.parameters["id"] ?: throw ValidationException("Payment id required")
                val request = call.receive<VerifyPaymentRequest>()
                val payment = paymentService.verify(
                    userId = principal.userId,
                    paymentId = id,
                    gatewayPaymentId = request.gatewayPaymentId,
                    signature = request.signature,
                )
                call.respond(PaymentResponse.from(payment))
            }

            post("/{id}/refund") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Payment id required")
                call.respond(PaymentResponse.from(paymentService.refund(actor, id)))
            }
        }
    }
}

private suspend fun RoutingContext.listPayments(paymentService: PaymentService): PaginatedResponse<PaymentResponse> {
    val principal = call.requirePrincipal()
    val params = call.request.queryParameters
    val page = params["page"]?.toIntOrNull() ?: 1
    val size = params["size"]?.toIntOrNull() ?: 20
    val result = paymentService.list(principal.userId, PageRequest(page = page, size = size))
    val effectiveSize = size.coerceIn(1, 100)
    val totalPages = if (result.total == 0L) 0 else ceil(result.total.toDouble() / effectiveSize).toInt()
    return PaginatedResponse(
        data = result.items.map(PaymentResponse::from),
        page = page.coerceAtLeast(1),
        size = effectiveSize,
        totalPages = totalPages,
    )
}

private fun ApplicationCall.requirePrincipal(): UserPrincipal =
    principal<UserPrincipal>() ?: throw UnauthorizedException()

private fun ApplicationCall.actor(): Actor {
    val principal = requirePrincipal()
    return Actor(userId = principal.userId, role = principal.role)
}
