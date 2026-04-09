package com.example.foodike.plugins

import com.example.foodike.auth.JwtConfig
import com.example.foodike.auth.JwtService
import com.example.foodike.auth.installJwtAuthentication
import com.example.foodike.di.serviceModules
import com.example.foodike.persistence.DatabaseConfig
import com.example.foodike.persistence.DatabaseFactory
import com.example.foodike.persistence.initializeSchema
import com.example.foodike.user.domain.service.AuthProperties
import com.example.foodike.user.infrastructure.auth.OtpProviderConfig
import com.example.foodike.user.infrastructure.persistence.RefreshTokensTable
import com.example.foodike.user.infrastructure.persistence.UsersTable
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.dsl.module
import java.time.Duration
import org.slf4j.LoggerFactory

fun Application.configureInfrastructure() {
    val logger = LoggerFactory.getLogger("InfraConfig")

    val database = initializeDatabase()
    val redisResources = initializeRedis(logger)
    initializeSchema(UsersTable, RefreshTokensTable)

    installDependencyInjection(database, redisResources)
    registerShutdownHooks(redisResources)
}

fun Application.configureAuthentication() {
    val koin = org.koin.core.context.GlobalContext.get()
    installJwtAuthentication(koin.get(), koin.get())
}

private fun Application.initializeDatabase(): Database {
    val databaseConfig = DatabaseConfig(
        jdbcUrl = configValue("database.jdbcUrl", "DB_URL"),
        driverClassName = configValue("database.driverClassName", "DB_DRIVER"),
        username = configValue("database.username", "DB_USER"),
        password = configValue("database.password", "DB_PASSWORD"),
        maximumPoolSize = configValue("database.maximumPoolSize", "DB_MAX_POOL_SIZE").toInt(),
    )
    return DatabaseFactory.create(databaseConfig)
}

private fun Application.initializeRedis(logger: org.slf4j.Logger): RedisResources? {
    val redisUrl = configValue("redis.url", "REDIS_URL")
    return try {
        val client = RedisClient.create(redisUrl)
        val connection: StatefulRedisConnection<String, String> = client.connect()
        RedisResources(client = client, connection = connection)
    } catch (exception: Exception) {
        logger.warn("Redis unavailable at {}. Falling back to in-memory OTP stores.", redisUrl)
        null
    }
}

private fun Application.installDependencyInjection(database: Database, redisResources: RedisResources?) {
    val jwtConfig = JwtConfig(
        secret = configValue("jwt.secret", "JWT_SECRET"),
        issuer = configValue("jwt.issuer", "JWT_ISSUER"),
        audience = configValue("jwt.audience", "JWT_AUDIENCE"),
        accessExpiryMillis = configValue("jwt.accessExpiryMillis", "JWT_ACCESS_EXPIRY_MILLIS").toLong(),
        refreshExpiryMillis = configValue("jwt.refreshExpiryMillis", "JWT_REFRESH_EXPIRY_MILLIS").toLong(),
    )
    val jwtService = JwtService(jwtConfig)
    val authProperties = AuthProperties(
        otpLength = configValue("auth.otp.length", "AUTH_OTP_LENGTH").toInt(),
        otpTtl = Duration.ofSeconds(configValue("auth.otp.ttlSeconds", "AUTH_OTP_TTL_SECONDS").toLong()),
        maxOtpSendsPerWindow = configValue("auth.otp.sendLimit", "AUTH_OTP_SEND_LIMIT").toInt(),
        otpSendWindow = Duration.ofSeconds(configValue("auth.otp.sendWindowSeconds", "AUTH_OTP_SEND_WINDOW_SECONDS").toLong()),
        maxOtpVerifyFailures = configValue("auth.otp.verifyFailureLimit", "AUTH_OTP_VERIFY_FAILURE_LIMIT").toInt(),
        otpVerifyWindow = Duration.ofSeconds(configValue("auth.otp.verifyWindowSeconds", "AUTH_OTP_VERIFY_WINDOW_SECONDS").toLong()),
        otpBlockDuration = Duration.ofSeconds(configValue("auth.otp.blockDurationSeconds", "AUTH_OTP_BLOCK_DURATION_SECONDS").toLong()),
    )
    val otpProviderConfig = OtpProviderConfig(
        provider = configValue("auth.otp.provider", "OTP_PROVIDER"),
        twilioAccountSid = configValue("auth.otp.twilio.accountSid", "TWILIO_ACCOUNT_SID").ifBlank { null },
        twilioAuthToken = configValue("auth.otp.twilio.authToken", "TWILIO_AUTH_TOKEN").ifBlank { null },
        twilioFromNumber = configValue("auth.otp.twilio.fromNumber", "TWILIO_FROM_NUMBER").ifBlank { null },
    )

    install(Koin) {
        slf4jLogger()
        modules(
            serviceModules +
                module {
                    single { database }
                    single { jwtConfig }
                    single { jwtService }
                    single { authProperties }
                    single { otpProviderConfig }
                } + listOfNotNull(
                    redisResources?.let { resources ->
                        module {
                            single { resources.client }
                            single<StatefulRedisConnection<String, String>> { resources.connection }
                        }
                    },
                ),
        )
    }
}

private fun Application.registerShutdownHooks(redisResources: RedisResources?) {
    monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        redisResources?.connection?.close()
        redisResources?.client?.shutdown()
    }
}

private fun Application.configValue(path: String, envVar: String): String =
    System.getenv(envVar)
        ?: environment.config.property(path).getString()

private data class RedisResources(
    val client: RedisClient,
    val connection: StatefulRedisConnection<String, String>,
)
