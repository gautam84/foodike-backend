package com.example.foodike.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.Date
import kotlinx.serialization.Serializable

@Serializable
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiryMillis: Long = 3_600_000,
)

@Serializable
enum class UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    ADMIN,
    DELIVERY,
}

data class UserPrincipal(
    val userId: String,
    val role: UserRole,
    val phone: String? = null,
) : Principal

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generateAccessToken(principal: UserPrincipal): String =
        JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", principal.userId)
            .withClaim("role", principal.role.name)
            .withClaim("phone", principal.phone)
            .withExpiresAt(Date(System.currentTimeMillis() + config.expiryMillis))
            .sign(algorithm)

    fun verifier() =
        JWT.require(algorithm)
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .build()
}

fun Application.installJwtAuthentication(jwtService: JwtService, config: JwtConfig) {
    authentication {
        jwt("auth-jwt") {
            realm = "foodike"
            verifier(jwtService.verifier())
            validate { credential ->
                val role = credential.payload.getClaim("role").asString()?.let(UserRole::valueOf)
                val userId = credential.payload.getClaim("userId").asString()

                if (role != null && userId != null) {
                    UserPrincipal(
                        userId = userId,
                        role = role,
                        phone = credential.payload.getClaim("phone").asString(),
                    )
                } else {
                    null
                }
            }
        }
    }
}

fun JWTPrincipal.toUserPrincipal(): UserPrincipal? {
    val roleValue = payload.getClaim("role").asString() ?: return null
    val userId = payload.getClaim("userId").asString() ?: return null

    return UserPrincipal(
        userId = userId,
        role = UserRole.valueOf(roleValue),
        phone = payload.getClaim("phone").asString(),
    )
}
