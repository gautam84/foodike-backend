package com.example.foodike.user.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.registerUserRoutes() {
    route("/auth") {
        get("/health") {
            call.respond(mapOf("service" to "user-service", "status" to "ready"))
        }
    }

    route("/users") {
        get("/me") {
            call.respond(mapOf("service" to "user-service", "message" to "profile module scaffolded"))
        }
    }
}
