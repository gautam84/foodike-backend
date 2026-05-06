package com.example.foodike.user.domain.service

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.model.User
import com.example.foodike.user.domain.port.SsoIdentityRepository
import com.example.foodike.user.domain.port.UserRepository
import java.net.URI

data class UserProfileView(
    val user: User,
    val ssoProviders: List<SsoProvider>,
)

class UserProfileService(
    private val userRepository: UserRepository,
    private val ssoIdentityRepository: SsoIdentityRepository,
) {
    suspend fun getProfile(userId: String): UserProfileView {
        val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        val providers = ssoIdentityRepository.findByUserId(userId).map { it.provider }
        return UserProfileView(user = user, ssoProviders = providers)
    }

    suspend fun updateProfile(
        userId: String,
        name: String?,
        email: String?,
        avatarUrl: String?,
    ): UserProfileView {
        val current = userRepository.findById(userId) ?: throw NotFoundException("User not found")

        val nextName = applyNameUpdate(current.name, name)
        val nextEmail = applyEmailUpdate(current.email, email, userId)
        val nextAvatar = applyAvatarUpdate(current.avatarUrl, avatarUrl)

        val updated = userRepository.save(
            current.copy(
                name = nextName,
                email = nextEmail,
                avatarUrl = nextAvatar,
            ),
        )

        val providers = ssoIdentityRepository.findByUserId(userId).map { it.provider }
        return UserProfileView(user = updated, ssoProviders = providers)
    }

    private fun applyNameUpdate(current: String?, requested: String?): String? {
        if (requested == null) return current
        val trimmed = requested.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > 100) {
            throw ValidationException(
                "Name must be 1-100 characters",
                mapOf("name" to "Maximum length is 100"),
            )
        }
        return trimmed
    }

    private suspend fun applyEmailUpdate(current: String?, requested: String?, userId: String): String? {
        if (requested == null) return current
        val trimmed = requested.trim()
        if (trimmed.isEmpty()) return null
        val lowered = trimmed.lowercase()
        if (!emailPattern.matches(lowered)) {
            throw ValidationException(
                "Email is not valid",
                mapOf("email" to "Provide a valid email address"),
            )
        }
        if (lowered != current?.lowercase()) {
            val existing = userRepository.findByEmail(lowered)
            if (existing != null && existing.id != userId) {
                throw ConflictException("Email already in use")
            }
        }
        return lowered
    }

    private fun applyAvatarUpdate(current: String?, requested: String?): String? {
        if (requested == null) return current
        val trimmed = requested.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > 2048) {
            throw ValidationException(
                "Avatar URL is too long",
                mapOf("avatarUrl" to "Maximum length is 2048"),
            )
        }
        val parsed = try {
            URI(trimmed)
        } catch (_: Exception) {
            throw ValidationException(
                "Avatar URL is not valid",
                mapOf("avatarUrl" to "Must be an absolute http(s) URL"),
            )
        }
        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw ValidationException(
                "Avatar URL must use http(s)",
                mapOf("avatarUrl" to "Scheme must be http or https"),
            )
        }
        if (parsed.host.isNullOrBlank()) {
            throw ValidationException(
                "Avatar URL must include a host",
                mapOf("avatarUrl" to "Host is required"),
            )
        }
        return trimmed
    }

    companion object {
        private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
