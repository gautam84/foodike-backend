package com.example.foodike.user.domain.model

import com.example.foodike.auth.UserRole
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val phone: String? = null,
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.CUSTOMER,
)

@Serializable
data class Address(
    val id: String,
    val userId: String,
    val label: String,
    val line1: String,
    val line2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false,
    @Serializable(with = com.example.foodike.user.domain.model.InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = com.example.foodike.user.domain.model.InstantSerializer::class)
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
