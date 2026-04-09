
package com.example.foodike.user.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.user.api.dto.AuthResponse
import com.example.foodike.user.api.dto.GoogleAuthRequest
import com.example.foodike.user.api.dto.LogoutRequest
import com.example.foodike.user.api.dto.MessageResponse
import com.example.foodike.user.api.dto.RefreshTokenRequest
import com.example.foodike.user.api.dto.RefreshTokenResponse
import com.example.foodike.user.api.dto.SendOtpRequest
import com.example.foodike.user.api.dto.VerifyOtpRequest
import com.example.foodike.user.domain.service.AuthService
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.registerUserRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {
        get("/health") {
            call.respond(mapOf("service" to "user-service", "status" to "ready"))
        }

        post("/send-otp") {
            val request = call.receive<SendOtpRequest>()
            authService.sendOtp(request.phone)
            call.respond(MessageResponse(message = "OTP sent"))
        }

        post("/verify-otp") {
            val request = call.receive<VerifyOtpRequest>()
            val result = authService.verifyOtp(phone = request.phone, otp = request.otp)
            call.respond(
                AuthResponse(
                    accessToken = result.tokens.accessToken,
                    refreshToken = result.tokens.refreshToken,
                    user = result.user,
                    isNewUser = result.isNewUser,
                ),
            )
        }

        post("/google") {
            val request = call.receive<GoogleAuthRequest>()
            val result = authService.authenticateWithGoogle(request.idToken)
            call.respond(
                AuthResponse(
                    accessToken = result.tokens.accessToken,
                    refreshToken = result.tokens.refreshToken,
                    user = result.user,
                    isNewUser = result.isNewUser,
                ),
            )
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val tokens = authService.refresh(request.refreshToken)
            call.respond(
                RefreshTokenResponse(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                ),
            )
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                val request = call.receive<LogoutRequest>()
                authService.logout(userId = principal.userId, refreshToken = request.refreshToken)
                call.respond(MessageResponse(message = "Logged out"))
            }

            get("/me") {
                val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                call.respond(authService.me(principal.userId))
            }
        }
    }

    route("/users") {
        get("/me") {
            call.respond(mapOf("service" to "user-service", "message" to "profile module scaffolded"))
        }
    }
}
