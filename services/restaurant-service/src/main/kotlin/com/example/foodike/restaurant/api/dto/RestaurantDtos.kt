package com.example.foodike.restaurant.api.dto

import com.example.foodike.restaurant.domain.model.Restaurant
import com.example.foodike.restaurant.domain.model.RestaurantHour
import kotlinx.serialization.Serializable

@Serializable
data class CreateRestaurantRequest(
    val name: String,
    val description: String? = null,
    val cuisines: List<String> = emptyList(),
    val phone: String? = null,
    val line1: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val deliveryFee: Int? = null,
    val minOrder: Int? = null,
    val prepTimeMins: Int? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class UpdateRestaurantRequest(
    val name: String? = null,
    val description: String? = null,
    val cuisines: List<String>? = null,
    val phone: String? = null,
    val line1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val deliveryFee: Int? = null,
    val minOrder: Int? = null,
    val prepTimeMins: Int? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class RestaurantResponse(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val cuisines: List<String>,
    val phone: String? = null,
    val line1: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val rating: Double,
    val ratingCount: Int,
    val deliveryFee: Int,
    val minOrder: Int,
    val prepTimeMins: Int,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(restaurant: Restaurant): RestaurantResponse =
            RestaurantResponse(
                id = restaurant.id,
                ownerId = restaurant.ownerId,
                name = restaurant.name,
                description = restaurant.description,
                cuisines = restaurant.cuisines,
                phone = restaurant.phone,
                line1 = restaurant.line1,
                city = restaurant.city,
                state = restaurant.state,
                postalCode = restaurant.postalCode,
                country = restaurant.country,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                imageUrl = restaurant.imageUrl,
                rating = restaurant.rating,
                ratingCount = restaurant.ratingCount,
                deliveryFee = restaurant.deliveryFee,
                minOrder = restaurant.minOrder,
                prepTimeMins = restaurant.prepTimeMins,
                enabled = restaurant.enabled,
                createdAt = restaurant.createdAt.toString(),
                updatedAt = restaurant.updatedAt.toString(),
            )
    }
}

@Serializable
data class HourDto(
    val dayOfWeek: Int,
    val opensAt: Int,
    val closesAt: Int,
) {
    companion object {
        fun from(hour: RestaurantHour): HourDto =
            HourDto(dayOfWeek = hour.dayOfWeek, opensAt = hour.opensAt, closesAt = hour.closesAt)
    }
}

@Serializable
data class SetHoursRequest(
    val hours: List<HourDto>,
)

@Serializable
data class HoursResponse(
    val hours: List<HourDto>,
)

@Serializable
data class RestaurantDetailResponse(
    val restaurant: RestaurantResponse,
    val hours: List<HourDto>,
    val menu: List<MenuCategoryResponse>,
)
