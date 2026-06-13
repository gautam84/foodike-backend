package com.example.foodike

import com.example.foodike.plugins.configureAuthentication
import com.example.foodike.plugins.configureCors
import com.example.foodike.plugins.configureInfrastructure
import com.example.foodike.plugins.configureMonitoring
import com.example.foodike.plugins.configureRateLimit
import com.example.foodike.plugins.configureRouting
import com.example.foodike.plugins.configureSerialization
import com.example.foodike.plugins.configureStatusPages
import com.example.foodike.plugins.configureWebSockets
import com.example.foodike.plugins.startNotificationConsumer
import com.example.foodike.plugins.startTrackingConsumer
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureCors()
    configureInfrastructure()
    configureAuthentication()
    startNotificationConsumer()
    startTrackingConsumer()
    configureStatusPages()
    configureRateLimit()
    configureWebSockets()
    configureRouting()
}
