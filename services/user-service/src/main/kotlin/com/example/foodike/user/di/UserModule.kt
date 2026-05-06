package com.example.foodike.user.di

import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.port.AddressRepository
import com.example.foodike.user.domain.port.OtpChallengeStore
import com.example.foodike.user.domain.port.OtpProvider
import com.example.foodike.user.domain.port.OtpRateLimitStore
import com.example.foodike.user.domain.port.RefreshTokenStore
import com.example.foodike.user.domain.port.SsoIdTokenVerifier
import com.example.foodike.user.domain.port.SsoIdentityRepository
import com.example.foodike.user.domain.port.UserRepository
import com.example.foodike.user.domain.service.AddressService
import com.example.foodike.user.domain.service.AuthProperties
import com.example.foodike.user.domain.service.AuthService
import com.example.foodike.user.domain.service.SsoVerifierRegistry
import com.example.foodike.user.domain.service.UserProfileService
import com.example.foodike.user.infrastructure.auth.ConsoleOtpProvider
import com.example.foodike.user.infrastructure.auth.GoogleIdTokenVerifierImpl
import com.example.foodike.user.infrastructure.auth.GoogleSsoConfig
import com.example.foodike.user.infrastructure.auth.InMemoryOtpChallengeStore
import com.example.foodike.user.infrastructure.auth.InMemoryOtpRateLimitStore
import com.example.foodike.user.infrastructure.auth.OtpProviderConfig
import com.example.foodike.user.infrastructure.auth.RedisOtpChallengeStore
import com.example.foodike.user.infrastructure.auth.RedisOtpRateLimitStore
import com.example.foodike.user.infrastructure.auth.StubSsoIdTokenVerifier
import com.example.foodike.user.infrastructure.auth.TwilioOtpProvider
import com.example.foodike.user.infrastructure.persistence.ExposedAddressRepository
import com.example.foodike.user.infrastructure.persistence.ExposedRefreshTokenStore
import com.example.foodike.user.infrastructure.persistence.ExposedSsoIdentityRepository
import com.example.foodike.user.infrastructure.persistence.ExposedUserRepository
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val userModule: Module = module {
    single<UserRepository> { ExposedUserRepository() }
    single<SsoIdentityRepository> { ExposedSsoIdentityRepository() }
    single<AddressRepository> { ExposedAddressRepository() }
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
    single<SsoIdTokenVerifier>(named("sso.google")) {
        val config: GoogleSsoConfig = get()
        when (config.mode.lowercase()) {
            "stub" -> StubSsoIdTokenVerifier(SsoProvider.GOOGLE)
            else -> GoogleIdTokenVerifierImpl(clientId = config.clientId)
        }
    }
    single<SsoVerifierRegistry> {
        SsoVerifierRegistry(
            verifiers = mapOf(
                SsoProvider.GOOGLE to get(named("sso.google")),
            ),
        )
    }
    single { UserProfileService(userRepository = get(), ssoIdentityRepository = get()) }
    single { AddressService(addressRepository = get()) }
    single {
        AuthService(
            jwtService = get(),
            userRepository = get(),
            otpChallengeStore = get(),
            otpRateLimitStore = get(),
            refreshTokenStore = get(),
            otpProvider = get(),
            ssoVerifierRegistry = get(),
            ssoIdentityRepository = get(),
            properties = get(),
        )
    }
}
