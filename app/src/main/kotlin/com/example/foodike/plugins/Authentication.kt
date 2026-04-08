package com.example.foodike.plugins

import com.example.foodike.auth.JwtConfig
import com.example.foodike.auth.JwtService
import com.example.foodike.auth.installJwtAuthentication
import com.example.foodike.di.serviceModules
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureAuthentication() {
    install(Koin) {
        slf4jLogger()
        modules(serviceModules)
    }

    val jwtConfig = JwtConfig(
        secret = environment.config.propertyOrNull("jwt.secret")?.getString() ?: "local-dev-secret",
        issuer = environment.config.propertyOrNull("jwt.issuer")?.getString() ?: "foodike",
        audience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "foodike-clients",
    )

    installJwtAuthentication(JwtService(jwtConfig), jwtConfig)
}
