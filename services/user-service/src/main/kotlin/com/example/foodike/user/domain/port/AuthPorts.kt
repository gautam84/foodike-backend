package com.example.foodike.user.domain.port

import com.example.foodike.user.domain.model.OtpChallenge
import com.example.foodike.user.domain.model.OtpRateLimitState
import com.example.foodike.user.domain.model.RefreshTokenRecord
import com.example.foodike.user.domain.model.User

interface OtpProvider {
    suspend fun sendOtp(phone: String, otp: String): Boolean
}

interface UserRepository {
    suspend fun findById(id: String): User?

    suspend fun findByPhone(phone: String): User?

    suspend fun findByEmail(email: String): User?

    suspend fun save(user: User): User
}

interface OtpChallengeStore {
    suspend fun save(challenge: OtpChallenge)

    suspend fun find(phone: String): OtpChallenge?

    suspend fun delete(phone: String)
}

interface OtpRateLimitStore {
    suspend fun incrementSendAttempts(phone: String): OtpRateLimitState

    suspend fun sendAttempts(phone: String): OtpRateLimitState?

    suspend fun incrementVerifyFailure(phone: String): OtpRateLimitState

    suspend fun verifyFailures(phone: String): OtpRateLimitState?

    suspend fun block(phone: String)

    suspend fun isBlocked(phone: String): Boolean

    suspend fun clearVerifyFailures(phone: String)
}

interface RefreshTokenStore {
    suspend fun save(record: RefreshTokenRecord)

    suspend fun findValidByUserId(userId: String): RefreshTokenRecord?

    suspend fun deleteByUserId(userId: String)
}
