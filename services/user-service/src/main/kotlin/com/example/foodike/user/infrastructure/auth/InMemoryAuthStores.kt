package com.example.foodike.user.infrastructure.auth

import com.example.foodike.user.domain.model.OtpChallenge
import com.example.foodike.user.domain.model.OtpRateLimitState
import com.example.foodike.user.domain.model.RefreshTokenRecord
import com.example.foodike.user.domain.model.User
import com.example.foodike.user.domain.port.OtpChallengeStore
import com.example.foodike.user.domain.port.OtpRateLimitStore
import com.example.foodike.user.domain.port.RefreshTokenStore
import com.example.foodike.user.domain.port.UserRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryUserRepository : UserRepository {
    private val mutex = Mutex()
    private val users = linkedMapOf<String, User>()

    override suspend fun findById(id: String): User? =
        mutex.withLock { users[id] }

    override suspend fun findByPhone(phone: String): User? =
        mutex.withLock { users.values.firstOrNull { it.phone == phone } }

    override suspend fun findByGoogleId(googleId: String): User? =
        mutex.withLock { users.values.firstOrNull { it.googleId == googleId } }

    override suspend fun findByEmail(email: String): User? =
        mutex.withLock { users.values.firstOrNull { it.email.equals(email, ignoreCase = true) } }

    override suspend fun save(user: User): User =
        mutex.withLock {
            val persisted = if (user.id.isBlank()) user.copy(id = UUID.randomUUID().toString()) else user
            users[persisted.id] = persisted
            persisted
        }
}

class InMemoryOtpChallengeStore : OtpChallengeStore {
    private val mutex = Mutex()
    private val challenges = mutableMapOf<String, OtpChallenge>()

    override suspend fun save(challenge: OtpChallenge) {
        mutex.withLock {
            challenges[challenge.phone] = challenge
        }
    }

    override suspend fun find(phone: String): OtpChallenge? =
        mutex.withLock {
            val challenge = challenges[phone]
            if (challenge != null && challenge.expiresAt.isAfter(Instant.now())) {
                challenge
            } else {
                challenges.remove(phone)
                null
            }
        }

    override suspend fun delete(phone: String) {
        mutex.withLock {
            challenges.remove(phone)
        }
    }
}

class InMemoryOtpRateLimitStore(
    private val sendWindow: Duration,
    private val verifyWindow: Duration,
    private val blockWindow: Duration,
) : OtpRateLimitStore {
    private val mutex = Mutex()
    private val sendCounts = mutableMapOf<String, OtpRateLimitState>()
    private val verifyCounts = mutableMapOf<String, OtpRateLimitState>()
    private val blockedPhones = mutableMapOf<String, Instant>()

    override suspend fun incrementSendAttempts(phone: String): OtpRateLimitState =
        incrementCounter(sendCounts, phone, sendWindow)

    override suspend fun sendAttempts(phone: String): OtpRateLimitState? =
        currentCounter(sendCounts, phone)

    override suspend fun incrementVerifyFailure(phone: String): OtpRateLimitState =
        incrementCounter(verifyCounts, phone, verifyWindow)

    override suspend fun verifyFailures(phone: String): OtpRateLimitState? =
        currentCounter(verifyCounts, phone)

    override suspend fun block(phone: String) {
        mutex.withLock {
            blockedPhones[phone] = Instant.now().plus(blockWindow)
        }
    }

    override suspend fun isBlocked(phone: String): Boolean =
        mutex.withLock {
            val blockedUntil = blockedPhones[phone] ?: return@withLock false
            if (blockedUntil.isAfter(Instant.now())) {
                true
            } else {
                blockedPhones.remove(phone)
                false
            }
        }

    override suspend fun clearVerifyFailures(phone: String) {
        mutex.withLock {
            verifyCounts.remove(phone)
            blockedPhones.remove(phone)
        }
    }

    private suspend fun incrementCounter(
        store: MutableMap<String, OtpRateLimitState>,
        phone: String,
        window: Duration,
    ): OtpRateLimitState =
        mutex.withLock {
            val now = Instant.now()
            val current = store[phone]?.takeIf { it.expiresAt.isAfter(now) }
            val next =
                if (current == null) {
                    OtpRateLimitState(count = 1, expiresAt = now.plus(window))
                } else {
                    current.copy(count = current.count + 1)
                }
            store[phone] = next
            next
        }

    private suspend fun currentCounter(
        store: MutableMap<String, OtpRateLimitState>,
        phone: String,
    ): OtpRateLimitState? =
        mutex.withLock {
            val state = store[phone] ?: return@withLock null
            if (state.expiresAt.isAfter(Instant.now())) {
                state
            } else {
                store.remove(phone)
                null
            }
        }
}

class InMemoryRefreshTokenStore : RefreshTokenStore {
    private val mutex = Mutex()
    private val tokensByUserId = mutableMapOf<String, RefreshTokenRecord>()

    override suspend fun save(record: RefreshTokenRecord) {
        mutex.withLock {
            tokensByUserId[record.userId] = record
        }
    }

    override suspend fun findValidByUserId(userId: String): RefreshTokenRecord? =
        mutex.withLock {
            val record = tokensByUserId[userId] ?: return@withLock null
            if (record.expiresAt.isAfter(Instant.now())) {
                record
            } else {
                tokensByUserId.remove(userId)
                null
            }
        }

    override suspend fun deleteByUserId(userId: String) {
        mutex.withLock {
            tokensByUserId.remove(userId)
        }
    }
}
