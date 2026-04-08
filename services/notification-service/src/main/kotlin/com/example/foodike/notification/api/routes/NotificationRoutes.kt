package com.example.foodike.notification.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.registerNotificationRoutes() {
    route("/notifications") {
        get("/health") {
            call.respond(mapOf("service" to "notification-service", "status" to "ready"))
        }
    }
}
