package com.example.foodike.restaurant.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.registerRestaurantRoutes() {
    route("/restaurants") {
        get {
            call.respond(mapOf("service" to "restaurant-service", "message" to "restaurant catalog scaffolded"))
        }
    }

    route("/search") {
        get {
            call.respond(mapOf("service" to "restaurant-service", "message" to "search scaffolded"))
        }
    }
}
