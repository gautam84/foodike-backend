package com.example.foodike.user.domain.port

import com.example.foodike.user.domain.model.SsoIdentity
import com.example.foodike.user.domain.model.SsoProfile
import com.example.foodike.user.domain.model.SsoProvider

interface SsoIdTokenVerifier {
    suspend fun verify(idToken: String): SsoProfile
}

interface SsoIdentityRepository {
    suspend fun findBySubject(provider: SsoProvider, subject: String): SsoIdentity?

    suspend fun findByUserId(userId: String): List<SsoIdentity>

    suspend fun save(identity: SsoIdentity): SsoIdentity

    suspend fun deleteByUserAndProvider(userId: String, provider: SsoProvider)
}
