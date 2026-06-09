package com.example.foodike.restaurant.domain.service

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.restaurant.domain.model.Restaurant
import com.example.foodike.restaurant.domain.model.RestaurantHour
import com.example.foodike.restaurant.domain.port.RestaurantFilter
import com.example.foodike.restaurant.domain.port.RestaurantPage
import com.example.foodike.restaurant.domain.port.RestaurantRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class CreateRestaurantInput(
    val name: String,
    val description: String?,
    val cuisines: List<String>,
    val phone: String?,
    val line1: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val deliveryFee: Int?,
    val minOrder: Int?,
    val prepTimeMins: Int?,
    val enabled: Boolean?,
)

data class UpdateRestaurantInput(
    val name: String?,
    val description: String?,
    val cuisines: List<String>?,
    val phone: String?,
    val line1: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val deliveryFee: Int?,
    val minOrder: Int?,
    val prepTimeMins: Int?,
    val enabled: Boolean?,
)

data class HourInput(
    val dayOfWeek: Int,
    val opensAt: Int,
    val closesAt: Int,
)

class RestaurantService(
    private val restaurantRepository: RestaurantRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun search(filter: RestaurantFilter, page: PageRequest): RestaurantPage =
        restaurantRepository.search(filter, normalizePage(page))

    suspend fun get(id: String): Restaurant =
        restaurantRepository.findById(id) ?: throw NotFoundException("Restaurant not found")

    suspend fun getHours(restaurantId: String): List<RestaurantHour> {
        get(restaurantId)
        return restaurantRepository.findHours(restaurantId)
    }

    suspend fun create(actor: Actor, input: CreateRestaurantInput): Restaurant {
        actor.requireManagerRole()
        val errors = mutableMapOf<String, String>()
        val name = requireText(input.name, "name", 150, errors)
        val line1 = requireText(input.line1, "line1", 255, errors)
        val city = requireText(input.city, "city", 100, errors)
        val state = requireText(input.state, "state", 100, errors)
        val postal = requirePostal(input.postalCode, errors)
        val country = requireCountry(input.country, errors)
        val cuisines = normalizeCuisines(input.cuisines)
        validateCoords(input.latitude, input.longitude, errors)
        validateNonNegative(input.deliveryFee, "deliveryFee", errors)
        validateNonNegative(input.minOrder, "minOrder", errors)
        validateNonNegative(input.prepTimeMins, "prepTimeMins", errors)
        if (errors.isNotEmpty()) throw ValidationException("Restaurant is not valid", errors)

        val now = Instant.now(clock)
        return restaurantRepository.create(
            Restaurant(
                id = UUID.randomUUID().toString(),
                ownerId = actor.userId,
                name = name,
                description = input.description?.trim()?.ifEmpty { null },
                cuisines = cuisines,
                phone = input.phone?.trim()?.ifEmpty { null },
                line1 = line1,
                city = city,
                state = state,
                postalCode = postal,
                country = country,
                latitude = input.latitude,
                longitude = input.longitude,
                imageUrl = input.imageUrl?.trim()?.ifEmpty { null },
                rating = 0.0,
                ratingCount = 0,
                deliveryFee = input.deliveryFee ?: 0,
                minOrder = input.minOrder ?: 0,
                prepTimeMins = input.prepTimeMins ?: 0,
                enabled = input.enabled ?: true,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(actor: Actor, id: String, input: UpdateRestaurantInput): Restaurant {
        val current = get(id)
        actor.requireCanManage(current)

        val errors = mutableMapOf<String, String>()
        val name = input.name?.let { requireText(it, "name", 150, errors) } ?: current.name
        val line1 = input.line1?.let { requireText(it, "line1", 255, errors) } ?: current.line1
        val city = input.city?.let { requireText(it, "city", 100, errors) } ?: current.city
        val state = input.state?.let { requireText(it, "state", 100, errors) } ?: current.state
        val postal = input.postalCode?.let { requirePostal(it, errors) } ?: current.postalCode
        val country = input.country?.let { requireCountry(it, errors) } ?: current.country
        val cuisines = input.cuisines?.let { normalizeCuisines(it) } ?: current.cuisines
        val lat = input.latitude ?: current.latitude
        val lon = input.longitude ?: current.longitude
        validateCoords(lat, lon, errors)
        validateNonNegative(input.deliveryFee, "deliveryFee", errors)
        validateNonNegative(input.minOrder, "minOrder", errors)
        validateNonNegative(input.prepTimeMins, "prepTimeMins", errors)
        if (errors.isNotEmpty()) throw ValidationException("Restaurant is not valid", errors)

        return restaurantRepository.update(
            current.copy(
                name = name,
                description = if (input.description == null) current.description else input.description.trim().ifEmpty { null },
                cuisines = cuisines,
                phone = if (input.phone == null) current.phone else input.phone.trim().ifEmpty { null },
                line1 = line1,
                city = city,
                state = state,
                postalCode = postal,
                country = country,
                latitude = lat,
                longitude = lon,
                imageUrl = if (input.imageUrl == null) current.imageUrl else input.imageUrl.trim().ifEmpty { null },
                deliveryFee = input.deliveryFee ?: current.deliveryFee,
                minOrder = input.minOrder ?: current.minOrder,
                prepTimeMins = input.prepTimeMins ?: current.prepTimeMins,
                enabled = input.enabled ?: current.enabled,
                updatedAt = Instant.now(clock),
            ),
        )
    }

    suspend fun delete(actor: Actor, id: String) {
        val current = get(id)
        actor.requireCanManage(current)
        restaurantRepository.delete(id)
    }

    suspend fun setHours(actor: Actor, restaurantId: String, hours: List<HourInput>): List<RestaurantHour> {
        val current = get(restaurantId)
        actor.requireCanManage(current)

        val errors = mutableMapOf<String, String>()
        val seenDays = mutableSetOf<Int>()
        hours.forEachIndexed { index, hour ->
            if (hour.dayOfWeek !in 0..6) errors["hours[$index].dayOfWeek"] = "Must be 0-6"
            if (hour.opensAt !in 0..1440) errors["hours[$index].opensAt"] = "Must be 0-1440 (minutes)"
            if (hour.closesAt !in 0..1440) errors["hours[$index].closesAt"] = "Must be 0-1440 (minutes)"
            if (hour.closesAt <= hour.opensAt) errors["hours[$index].closesAt"] = "Must be after opensAt"
            if (!seenDays.add(hour.dayOfWeek)) errors["hours[$index].dayOfWeek"] = "Duplicate day"
        }
        if (errors.isNotEmpty()) throw ValidationException("Hours are not valid", errors)

        return restaurantRepository.replaceHours(
            restaurantId,
            hours.map {
                RestaurantHour(
                    id = UUID.randomUUID().toString(),
                    restaurantId = restaurantId,
                    dayOfWeek = it.dayOfWeek,
                    opensAt = it.opensAt,
                    closesAt = it.closesAt,
                )
            },
        )
    }

    private fun normalizePage(page: PageRequest): PageRequest =
        page.copy(
            page = page.page.coerceAtLeast(1),
            size = page.size.coerceIn(1, 100),
        )

    private fun normalizeCuisines(cuisines: List<String>): List<String> =
        cuisines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()

    private fun requireText(value: String, field: String, max: Int, errors: MutableMap<String, String>): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > max) errors[field] = "Length must be 1-$max"
        return trimmed
    }

    private fun requirePostal(value: String, errors: MutableMap<String, String>): String {
        val trimmed = value.trim()
        if (trimmed.length !in 3..20) errors["postalCode"] = "Length must be 3-20"
        return trimmed
    }

    private fun requireCountry(value: String, errors: MutableMap<String, String>): String {
        val upper = value.trim().uppercase()
        if (!Regex("^[A-Z]{2}$").matches(upper)) errors["country"] = "Must be ISO 3166-1 alpha-2"
        return upper
    }

    private fun validateCoords(lat: Double?, lon: Double?, errors: MutableMap<String, String>) {
        if (lat != null && lat !in -90.0..90.0) errors["latitude"] = "Must be in [-90, 90]"
        if (lon != null && lon !in -180.0..180.0) errors["longitude"] = "Must be in [-180, 180]"
    }

    private fun validateNonNegative(value: Int?, field: String, errors: MutableMap<String, String>) {
        if (value != null && value < 0) errors[field] = "Must be >= 0"
    }
}
