package com.example.foodike.tracking.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Route.registerTrackingRoutes() {
    route("/track") {
        get("/health") {
            call.respond(mapOf("service" to "tracking-service", "status" to "ready"))
        }
    }

    webSocket("/track/{orderId}") {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                send(Frame.Text("tracking:${frame.readText()}"))
            }
        }
    }
}
