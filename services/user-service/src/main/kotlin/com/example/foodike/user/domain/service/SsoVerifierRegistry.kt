package com.example.foodike.user.domain.service

import com.example.foodike.common.exception.ValidationException
import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.port.SsoIdTokenVerifier

class SsoVerifierRegistry(
    private val verifiers: Map<SsoProvider, SsoIdTokenVerifier>,
) {
    fun forProvider(provider: SsoProvider): SsoIdTokenVerifier =
        verifiers[provider]
            ?: throw ValidationException("SSO provider not supported: ${provider.name.lowercase()}")

    fun supportedProviders(): Set<SsoProvider> = verifiers.keys
}
