package com.example.foodike.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import java.util.Date
import java.util.UUID
import kotlinx.serialization.Serializable

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessExpiryMillis: Long = 900_000,
    val refreshExpiryMillis: Long = 2_592_000_000,
)

enum class TokenType {
    ACCESS,
    REFRESH,
}

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
)

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generateAccessToken(principal: UserPrincipal): String =
        generateToken(principal = principal, tokenType = TokenType.ACCESS, expiryMillis = config.accessExpiryMillis)

    fun generateRefreshToken(principal: UserPrincipal):
            String =
        generateToken(principal = principal, tokenType = TokenType.REFRESH, expiryMillis = config.refreshExpiryMillis)

    private fun generateToken(
        principal: UserPrincipal,
        tokenType: TokenType,
        expiryMillis: Long,
    ): String =
        JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", principal.userId)
            .withClaim("role", principal.role.name)
            .withClaim("phone", principal.phone)
            .withClaim("type", tokenType.name)
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date(System.currentTimeMillis() + expiryMillis))
            .sign(algorithm)

    fun verifier(tokenType: TokenType) =
        JWT.require(algorithm)
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("type", tokenType.name)
            .build()

    fun decode(token: String): DecodedJWT = JWT.decode(token)
}

fun Application.installJwtAuthentication(jwtService: JwtService, config: JwtConfig) {
    authentication {
        jwt("auth-jwt") {
            realm = "foodike"
            verifier(jwtService.verifier(TokenType.ACCESS))
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
