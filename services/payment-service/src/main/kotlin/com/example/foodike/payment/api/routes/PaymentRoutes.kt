package com.example.foodike.payment.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.registerPaymentRoutes() {
    route("/payments") {
        get("/health") {
            call.respond(mapOf("service" to "payment-service", "status" to "ready"))
        }
    }
}
