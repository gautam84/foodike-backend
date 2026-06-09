package com.example.foodike.restaurant.domain.model

import com.example.foodike.common.model.Money
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String,
    val ownerId: String,
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
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val deliveryFee: Int = 0,
    val minOrder: Int = 0,
    val prepTimeMins: Int = 0,
    val enabled: Boolean = true,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

@Serializable
data class RestaurantHour(
    val id: String,
    val restaurantId: String,
    val dayOfWeek: Int,
    val opensAt: Int,
    val closesAt: Int,
)

@Serializable
data class MenuCategory(
    val id: String,
    val restaurantId: String,
    val name: String,
    val sortOrder: Int = 0,
    val items: List<MenuItem> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

@Serializable
data class MenuItem(
    val id: String,
    val categoryId: String,
    val restaurantId: String,
    val name: String,
    val description: String? = null,
    val price: Money,
    val isVeg: Boolean = false,
    val isAvailable: Boolean = true,
    val imageUrl: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

@Serializable
data class Review(
    val id: String,
    val restaurantId: String,
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

internal object InstantSerializer : kotlinx.serialization.KSerializer<Instant> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            "java.time.Instant",
            kotlinx.serialization.descriptors.PrimitiveKind.STRING,
        )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Instant =
        Instant.parse(decoder.decodeString())
}
