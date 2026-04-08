package com.example.foodike.plugins

import com.example.foodike.notification.api.routes.registerNotificationRoutes
import com.example.foodike.order.api.routes.registerOrderRoutes
import com.example.foodike.restaurant.api.routes.registerRestaurantRoutes
import com.example.foodike.tracking.api.routes.registerTrackingRoutes
import com.example.foodike.user.api.routes.registerUserRoutes
import com.example.foodike.payment.api.routes.registerPaymentRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(mapOf("name" to "foodike-backend", "architecture" to "modular-monolith"))
        }

        route("/api/v1") {
            registerUserRoutes()
            registerRestaurantRoutes()
            registerOrderRoutes()
            registerPaymentRoutes()
            registerNotificationRoutes()
            registerTrackingRoutes()
        }
    }
}
