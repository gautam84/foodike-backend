package com.example.foodike.tracking.domain.model

import com.example.foodike.common.model.Location
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryStatus {
    PENDING_ASSIGNMENT,
    ASSIGNED,
    PICKED_UP,
    EN_ROUTE,
    ARRIVED,
    DELIVERED,
    CANCELLED,
}

/**
 * Live delivery-tracking state for a single order. Keyed by [orderId] (one session per order).
 * Only the most recent courier position is retained ([latestLocation]); the live stream of every
 * ping is fanned out separately through the broadcaster.
 */
@Serializable
data class TrackingSession(
    val orderId: String,
    val customerId: String,
    val courierId: String? = null,
    val status: DeliveryStatus = DeliveryStatus.PENDING_ASSIGNMENT,
    val latestLocation: Location? = null,
    @Serializable(with = InstantSerializer::class)
    val locationUpdatedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

/** A single broadcast frame pushed to live subscribers as the delivery moves or changes status. */
@Serializable
data class TrackingUpdate(
    val orderId: String,
    val status: DeliveryStatus,
    val location: Location? = null,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
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
