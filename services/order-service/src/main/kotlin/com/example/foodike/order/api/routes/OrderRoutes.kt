package com.example.foodike.order.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.registerOrderRoutes() {
    route("/cart") {
        get {
            call.respond(mapOf("service" to "order-service", "message" to "cart scaffolded"))
        }
    }

    route("/orders") {
        get {
            call.respond(mapOf("service" to "order-service", "message" to "order flow scaffolded"))
        }
    }
}
