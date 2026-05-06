
package com.example.foodike.user.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.user.api.dto.AddressListResponse
import com.example.foodike.user.api.dto.AddressResponse
import com.example.foodike.user.api.dto.AuthResponse
import com.example.foodike.user.api.dto.CreateAddressRequest
import com.example.foodike.user.api.dto.GoogleAuthRequest
import com.example.foodike.user.api.dto.LogoutRequest
import com.example.foodike.user.api.dto.MessageResponse
import com.example.foodike.user.api.dto.RefreshTokenRequest
import com.example.foodike.user.api.dto.RefreshTokenResponse
import com.example.foodike.user.api.dto.SendOtpRequest
import com.example.foodike.user.api.dto.SsoAuthRequest
import com.example.foodike.user.api.dto.UpdateAddressRequest
import com.example.foodike.user.api.dto.UpdateProfileRequest
import com.example.foodike.user.api.dto.UserProfileResponse
import com.example.foodike.user.api.dto.VerifyOtpRequest
import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.service.AddressService
import com.example.foodike.user.domain.service.AuthService
import com.example.foodike.user.domain.service.CreateAddressInput
import com.example.foodike.user.domain.service.UpdateAddressInput
import com.example.foodike.user.domain.service.UserProfileService
import com.example.foodike.user.domain.service.UserProfileView
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.registerUserRoutes() {
    val authService by inject<AuthService>()
    val userProfileService by inject<UserProfileService>()
    val addressService by inject<AddressService>()

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

        post("/sso/{provider}") {
            val providerParam = call.parameters["provider"]
                ?: throw ValidationException("SSO provider is required")
            val provider = parseProvider(providerParam)
            val request = call.receive<SsoAuthRequest>()
            val result = authService.authenticateWithSso(provider, request.idToken)
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
            val result = authService.authenticateWithSso(SsoProvider.GOOGLE, request.idToken)
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
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                call.respond(userProfileService.getProfile(principal.userId).toResponse())
            }

            patch("/me") {
                val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                val request = call.receive<UpdateProfileRequest>()
                val updated = userProfileService.updateProfile(
                    userId = principal.userId,
                    name = request.name,
                    email = request.email,
                    avatarUrl = request.avatarUrl,
                )
                call.respond(updated.toResponse())
            }

            route("/me/addresses") {
                get {
                    val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                    val addresses = addressService.list(principal.userId).map(AddressResponse::from)
                    call.respond(AddressListResponse(addresses))
                }

                post {
                    val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                    val request = call.receive<CreateAddressRequest>()
                    val created = addressService.create(
                        userId = principal.userId,
                        input = CreateAddressInput(
                            label = request.label,
                            line1 = request.line1,
                            line2 = request.line2,
                            city = request.city,
                            state = request.state,
                            postalCode = request.postalCode,
                            country = request.country,
                            latitude = request.latitude,
                            longitude = request.longitude,
                            isDefault = request.isDefault,
                        ),
                    )
                    call.respond(HttpStatusCode.Created, AddressResponse.from(created))
                }

                get("/{id}") {
                    val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                    val id = call.parameters["id"] ?: throw ValidationException("Address id required")
                    val address = addressService.get(principal.userId, id)
                    call.respond(AddressResponse.from(address))
                }

                patch("/{id}") {
                    val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                    val id = call.parameters["id"] ?: throw ValidationException("Address id required")
                    val request = call.receive<UpdateAddressRequest>()
                    val updated = addressService.update(
                        userId = principal.userId,
                        addressId = id,
                        input = UpdateAddressInput(
                            label = request.label,
                            line1 = request.line1,
                            line2 = request.line2,
                            city = request.city,
                            state = request.state,
                            postalCode = request.postalCode,
                            country = request.country,
                            latitude = request.latitude,
                            longitude = request.longitude,
                        ),
                    )
                    call.respond(AddressResponse.from(updated))
                }

                delete("/{id}") {
                    val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                    val id = call.parameters["id"] ?: throw ValidationException("Address id required")
                    addressService.delete(principal.userId, id)
                    call.respond(HttpStatusCode.NoContent)
                }

                post("/{id}/default") {
                    val principal = call.principal<UserPrincipal>() ?: throw UnauthorizedException()
                    val id = call.parameters["id"] ?: throw ValidationException("Address id required")
                    val updated = addressService.setDefault(principal.userId, id)
                    call.respond(AddressResponse.from(updated))
                }
            }
        }
    }
}

private fun parseProvider(raw: String): SsoProvider {
    val normalized = raw.trim().uppercase()
    return runCatching { SsoProvider.valueOf(normalized) }
        .getOrElse {
            throw ValidationException(
                "Unsupported SSO provider: ${raw.lowercase()}",
                mapOf("provider" to "Supported: ${SsoProvider.entries.joinToString { it.name.lowercase() }}"),
            )
        }
}

private fun UserProfileView.toResponse(): UserProfileResponse =
    UserProfileResponse(
        id = user.id,
        phone = user.phone,
        email = user.email,
        name = user.name,
        avatarUrl = user.avatarUrl,
        role = user.role,
        ssoProviders = ssoProviders,
    )
