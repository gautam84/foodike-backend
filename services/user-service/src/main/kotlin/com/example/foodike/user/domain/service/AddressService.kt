package com.example.foodike.user.domain.service

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.user.domain.model.Address
import com.example.foodike.user.domain.port.AddressRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class CreateAddressInput(
    val label: String,
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double?,
    val longitude: Double?,
    val isDefault: Boolean?,
)

data class UpdateAddressInput(
    val label: String?,
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
)

class AddressService(
    private val addressRepository: AddressRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun list(userId: String): List<Address> = addressRepository.findByUserId(userId)

    suspend fun get(userId: String, addressId: String): Address =
        addressRepository.findByIdForUser(addressId, userId)
            ?: throw NotFoundException("Address not found")

    suspend fun create(userId: String, input: CreateAddressInput): Address {
        val errors = mutableMapOf<String, String>()
        val label = requireLabel(input.label, errors)
        val line1 = requireLine(input.line1, "line1", 255, errors)
        val line2 = input.line2?.let { optionalLine(it, "line2", 255, errors) }
        val city = requireSimple(input.city, "city", 100, errors)
        val state = requireSimple(input.state, "state", 100, errors)
        val postal = requirePostal(input.postalCode, errors)
        val country = requireCountry(input.country, errors)
        validateCoords(input.latitude, input.longitude, errors)
        if (errors.isNotEmpty()) throw ValidationException("Address is not valid", errors)

        val isFirst = addressRepository.countForUser(userId) == 0L
        val isDefault = input.isDefault ?: isFirst
        val nowInstant = Instant.now(clock)

        return addressRepository.create(
            Address(
                id = UUID.randomUUID().toString(),
                userId = userId,
                label = label,
                line1 = line1,
                line2 = line2,
                city = city,
                state = state,
                postalCode = postal,
                country = country,
                latitude = input.latitude,
                longitude = input.longitude,
                isDefault = isDefault || isFirst,
                createdAt = nowInstant,
                updatedAt = nowInstant,
            ),
        )
    }

    suspend fun update(userId: String, addressId: String, input: UpdateAddressInput): Address {
        val current = addressRepository.findByIdForUser(addressId, userId)
            ?: throw NotFoundException("Address not found")

        val errors = mutableMapOf<String, String>()
        val label = input.label?.let { requireLabel(it, errors) } ?: current.label
        val line1 = input.line1?.let { requireLine(it, "line1", 255, errors) } ?: current.line1
        val line2 = if (input.line2 == null) current.line2 else optionalLine(input.line2, "line2", 255, errors)
        val city = input.city?.let { requireSimple(it, "city", 100, errors) } ?: current.city
        val state = input.state?.let { requireSimple(it, "state", 100, errors) } ?: current.state
        val postal = input.postalCode?.let { requirePostal(it, errors) } ?: current.postalCode
        val country = input.country?.let { requireCountry(it, errors) } ?: current.country
        val lat = input.latitude ?: current.latitude
        val lon = input.longitude ?: current.longitude
        validateCoords(lat, lon, errors)
        if (errors.isNotEmpty()) throw ValidationException("Address is not valid", errors)

        return addressRepository.update(
            current.copy(
                label = label,
                line1 = line1,
                line2 = line2,
                city = city,
                state = state,
                postalCode = postal,
                country = country,
                latitude = lat,
                longitude = lon,
                updatedAt = Instant.now(clock),
            ),
        )
    }

    suspend fun delete(userId: String, addressId: String) {
        val deleted = addressRepository.delete(addressId, userId)
        if (!deleted) throw NotFoundException("Address not found")
    }

    suspend fun setDefault(userId: String, addressId: String): Address =
        addressRepository.setDefault(addressId, userId)

    private fun requireLabel(value: String, errors: MutableMap<String, String>): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > 50) {
            errors["label"] = "Length must be 1-50"
        }
        return trimmed
    }

    private fun requireLine(value: String, field: String, max: Int, errors: MutableMap<String, String>): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > max) {
            errors[field] = "Length must be 1-$max"
        }
        return trimmed
    }

    private fun optionalLine(value: String, field: String, max: Int, errors: MutableMap<String, String>): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > max) errors[field] = "Maximum length is $max"
        return trimmed
    }

    private fun requireSimple(value: String, field: String, max: Int, errors: MutableMap<String, String>): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > max) {
            errors[field] = "Length must be 1-$max"
        }
        return trimmed
    }

    private fun requirePostal(value: String, errors: MutableMap<String, String>): String {
        val trimmed = value.trim()
        if (trimmed.length !in 3..20) {
            errors["postalCode"] = "Length must be 3-20"
        }
        return trimmed
    }

    private fun requireCountry(value: String, errors: MutableMap<String, String>): String {
        val upper = value.trim().uppercase()
        if (!Regex("^[A-Z]{2}$").matches(upper)) {
            errors["country"] = "Must be ISO 3166-1 alpha-2"
        }
        return upper
    }

    private fun validateCoords(lat: Double?, lon: Double?, errors: MutableMap<String, String>) {
        if (lat != null && lat !in -90.0..90.0) errors["latitude"] = "Must be in [-90, 90]"
        if (lon != null && lon !in -180.0..180.0) errors["longitude"] = "Must be in [-180, 180]"
    }
}
