package com.example.foodike.user.di

import com.example.foodike.user.domain.port.GoogleIdTokenVerifier
import com.example.foodike.user.domain.port.OtpChallengeStore
import com.example.foodike.user.domain.port.OtpProvider
import com.example.foodike.user.domain.port.OtpRateLimitStore
import com.example.foodike.user.domain.port.RefreshTokenStore
import com.example.foodike.user.domain.port.UserRepository
import com.example.foodike.user.domain.service.AuthProperties
import com.example.foodike.user.domain.service.AuthService
import com.example.foodike.user.infrastructure.auth.ConsoleOtpProvider
import com.example.foodike.user.infrastructure.auth.InMemoryOtpChallengeStore
import com.example.foodike.user.infrastructure.auth.InMemoryOtpRateLimitStore
import com.example.foodike.user.infrastructure.auth.OtpProviderConfig
import com.example.foodike.user.infrastructure.auth.RedisOtpChallengeStore
import com.example.foodike.user.infrastructure.auth.RedisOtpRateLimitStore
import com.example.foodike.user.infrastructure.auth.TwilioOtpProvider
import com.example.foodike.user.infrastructure.auth.UnsupportedGoogleIdTokenVerifier
import com.example.foodike.user.infrastructure.persistence.ExposedRefreshTokenStore
import com.example.foodike.user.infrastructure.persistence.ExposedUserRepository
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.core.module.Module
import org.koin.dsl.module

val userModule: Module = module {
    single<UserRepository> { ExposedUserRepository() }
    single<OtpChallengeStore> {
        val redisConnection = getOrNull<StatefulRedisConnection<String, String>>()
        if (redisConnection != null) {
            RedisOtpChallengeStore(connection = redisConnection)
        } else {
            InMemoryOtpChallengeStore()
        }
    }
    single<OtpRateLimitStore> {
        val properties: AuthProperties = get()
        val redisConnection = getOrNull<StatefulRedisConnection<String, String>>()
        if (redisConnection != null) {
            RedisOtpRateLimitStore(
                connection = redisConnection,
                sendWindow = properties.otpSendWindow,
                verifyWindow = properties.otpVerifyWindow,
                blockWindow = properties.otpBlockDuration,
            )
        } else {
            InMemoryOtpRateLimitStore(
                sendWindow = properties.otpSendWindow,
                verifyWindow = properties.otpVerifyWindow,
                blockWindow = properties.otpBlockDuration,
            )
        }
    }
    single<RefreshTokenStore> { ExposedRefreshTokenStore() }
    single { ConsoleOtpProvider() }
    single<OtpProvider> {
        when (get<OtpProviderConfig>().provider.lowercase()) {
            "twilio" -> TwilioOtpProvider(get())
            else -> get<ConsoleOtpProvider>()
        }
    }
    single<GoogleIdTokenVerifier> { UnsupportedGoogleIdTokenVerifier() }
    single {
        AuthService(
            jwtService = get(),
            userRepository = get(),
            otpChallengeStore = get(),
            otpRateLimitStore = get(),
            refreshTokenStore = get(),
            otpProvider = get(),
            googleIdTokenVerifier = get(),
            properties = get(),
        )
    }
}
