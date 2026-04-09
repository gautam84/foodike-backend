package com.example.foodike.user.infrastructure.auth

import com.example.foodike.user.domain.model.OtpChallenge
import com.example.foodike.user.domain.model.OtpRateLimitState
import com.example.foodike.user.domain.port.OtpChallengeStore
import com.example.foodike.user.domain.port.OtpRateLimitStore
import io.lettuce.core.api.StatefulRedisConnection
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val OTP_KEY_PREFIX = "otp:"
private const val OTP_RATE_KEY_PREFIX = "otp:rate:"
private const val OTP_FAIL_KEY_PREFIX = "otp:fail:"
private const val OTP_BLOCK_KEY_PREFIX = "otp:block:"

@Serializable
private data class StoredOtpChallenge(
    val hash: String,
    val expiresAtEpochMillis: Long,
)

class RedisOtpChallengeStore(
    private val connection: StatefulRedisConnection<String, String>,
    private val json: Json = Json,
) : OtpChallengeStore {
    override suspend fun save(challenge: OtpChallenge) {
        connection.withCommands { commands ->
            val ttlSeconds = Duration.between(Instant.now(), challenge.expiresAt).seconds.coerceAtLeast(1)
            commands.setex(
                otpKey(challenge.phone),
                ttlSeconds,
                json.encodeToString(
                    StoredOtpChallenge(
                        hash = challenge.codeHash,
                        expiresAtEpochMillis = challenge.expiresAt.toEpochMilli(),
                    ),
                ),
            )
        }
    }

    override suspend fun find(phone: String): OtpChallenge? =
        connection.withCommands { commands ->
            val payload = commands.get(otpKey(phone)) ?: return@withCommands null
            val stored = json.decodeFromString<StoredOtpChallenge>(payload)
            val expiresAt = Instant.ofEpochMilli(stored.expiresAtEpochMillis)
            if (expiresAt.isAfter(Instant.now())) {
                OtpChallenge(phone = phone, codeHash = stored.hash, expiresAt = expiresAt)
            } else {
                commands.del(otpKey(phone))
                null
            }
        }

    override suspend fun delete(phone: String) {
        connection.withCommands { commands ->
            commands.del(otpKey(phone))
        }
    }
}

class RedisOtpRateLimitStore(
    private val connection: StatefulRedisConnection<String, String>,
    private val sendWindow: Duration,
    private val verifyWindow: Duration,
    private val blockWindow: Duration,
) : OtpRateLimitStore {
    override suspend fun incrementSendAttempts(phone: String): OtpRateLimitState =
        incrementCounter(rateKey(phone), sendWindow)

    override suspend fun sendAttempts(phone: String): OtpRateLimitState? =
        currentCounter(rateKey(phone))

    override suspend fun incrementVerifyFailure(phone: String): OtpRateLimitState =
        incrementCounter(failKey(phone), verifyWindow)

    override suspend fun verifyFailures(phone: String): OtpRateLimitState? =
        currentCounter(failKey(phone))

    override suspend fun block(phone: String) {
        connection.withCommands { commands ->
            commands.setex(blockKey(phone), blockWindow.seconds.coerceAtLeast(1), "1")
        }
    }

    override suspend fun isBlocked(phone: String): Boolean =
        connection.withCommands { commands ->
            commands.exists(blockKey(phone)) > 0
        }

    override suspend fun clearVerifyFailures(phone: String) {
        connection.withCommands { commands ->
            commands.del(failKey(phone), blockKey(phone))
        }
    }

    private suspend fun incrementCounter(key: String, window: Duration): OtpRateLimitState =
        connection.withCommands { commands ->
            val count = commands.incr(key).toInt()
            if (count == 1) {
                commands.expire(key, window.seconds.coerceAtLeast(1))
            }
            val ttl = commands.ttl(key).coerceAtLeast(0)
            OtpRateLimitState(
                count = count,
                expiresAt = Instant.now().plusSeconds(ttl),
            )
        }

    private suspend fun currentCounter(key: String): OtpRateLimitState? =
        connection.withCommands { commands ->
            val value = commands.get(key)?.toIntOrNull() ?: return@withCommands null
            val ttl = commands.ttl(key)
            if (ttl <= 0) {
                commands.del(key)
                null
            } else {
                OtpRateLimitState(
                    count = value,
                    expiresAt = Instant.now().plusSeconds(ttl),
                )
            }
        }

    private fun rateKey(phone: String) = "$OTP_RATE_KEY_PREFIX$phone"

    private fun failKey(phone: String) = "$OTP_FAIL_KEY_PREFIX$phone"

    private fun blockKey(phone: String) = "$OTP_BLOCK_KEY_PREFIX$phone"
}

private fun otpKey(phone: String) = "$OTP_KEY_PREFIX$phone"

private suspend fun <T> StatefulRedisConnection<String, String>.withCommands(
    block: (io.lettuce.core.api.sync.RedisCommands<String, String>) -> T,
): T =
    withContext(Dispatchers.IO) {
        block(sync())
    }
