package com.example.foodike.tracking.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.DomainException
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.Location
import com.example.foodike.tracking.api.dto.AdvanceStatusRequest
import com.example.foodike.tracking.api.dto.AssignCourierRequest
import com.example.foodike.tracking.api.dto.LocationUpdateRequest
import com.example.foodike.tracking.api.dto.TrackingSessionResponse
import com.example.foodike.tracking.api.dto.TrackingUpdateMessage
import com.example.foodike.tracking.domain.port.TrackingBroadcaster
import com.example.foodike.tracking.domain.service.Actor
import com.example.foodike.tracking.domain.service.TrackingService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

private val trackingJson = Json { encodeDefaults = true }

fun Route.registerTrackingRoutes() {
    val trackingService by inject<TrackingService>()
    val broadcaster by inject<TrackingBroadcaster>()

    route("/track") {
        get("/health") {
            call.respond(mapOf("service" to "tracking-service", "status" to "ready"))
        }
    }

    authenticate("auth-jwt") {
        route("/track") {
            get("/{orderId}") {
                val actor = call.actor()
                val orderId = call.requireOrderId()
                call.respond(TrackingSessionResponse.from(trackingService.getSession(actor, orderId)))
            }

            post("/{orderId}/courier") {
                val actor = call.actor()
                val orderId = call.requireOrderId()
                val request = call.receive<AssignCourierRequest>()
                val session = trackingService.assignCourier(actor, orderId, request.courierId)
                call.respond(TrackingSessionResponse.from(session))
            }

            post("/{orderId}/location") {
                val actor = call.actor()
                val orderId = call.requireOrderId()
                val request = call.receive<LocationUpdateRequest>()
                val session = trackingService.recordLocation(actor, orderId, Location(request.lat, request.lng))
                call.respond(TrackingSessionResponse.from(session))
            }

            patch("/{orderId}/status") {
                val actor = call.actor()
                val orderId = call.requireOrderId()
                val request = call.receive<AdvanceStatusRequest>()
                val session = trackingService.advanceStatus(actor, orderId, request.status)
                call.respond(TrackingSessionResponse.from(session))
            }

            webSocket("/{orderId}/live") {
                val orderId = call.parameters["orderId"]
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Order id required"))

                // Authorize and emit the current snapshot before streaming live updates.
                val snapshot = try {
                    trackingService.getSession(call.actor(), orderId)
                } catch (exception: DomainException) {
                    return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, exception.message ?: "Forbidden"))
                }
                send(Frame.Text(trackingJson.encodeToString(TrackingUpdateMessage.from(snapshot))))

                broadcaster.stream(orderId).collect { update ->
                    send(Frame.Text(trackingJson.encodeToString(TrackingUpdateMessage.from(update))))
                }
            }
        }
    }
}

private fun ApplicationCall.requireOrderId(): String =
    parameters["orderId"] ?: throw ValidationException("Order id required")

private fun ApplicationCall.requirePrincipal(): UserPrincipal =
    principal<UserPrincipal>() ?: throw UnauthorizedException()

private fun ApplicationCall.actor(): Actor {
    val principal = requirePrincipal()
    return Actor(userId = principal.userId, role = principal.role)
}
