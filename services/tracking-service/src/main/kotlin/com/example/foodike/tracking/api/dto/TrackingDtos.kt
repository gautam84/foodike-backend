package com.example.foodike.tracking.api.dto

import com.example.foodike.common.model.Location
import com.example.foodike.tracking.domain.model.DeliveryStatus
import com.example.foodike.tracking.domain.model.TrackingSession
import com.example.foodike.tracking.domain.model.TrackingUpdate
import kotlinx.serialization.Serializable

@Serializable
data class LocationUpdateRequest(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class AssignCourierRequest(
    val courierId: String,
)

@Serializable
data class AdvanceStatusRequest(
    val status: DeliveryStatus,
)

@Serializable
data class LocationDto(
    val lat: Double,
    val lng: Double,
) {
    companion object {
        fun from(location: Location): LocationDto = LocationDto(lat = location.lat, lng = location.lng)
    }
}

@Serializable
data class TrackingSessionResponse(
    val orderId: String,
    val customerId: String,
    val courierId: String? = null,
    val status: DeliveryStatus,
    val location: LocationDto? = null,
    val locationUpdatedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(session: TrackingSession): TrackingSessionResponse =
            TrackingSessionResponse(
                orderId = session.orderId,
                customerId = session.customerId,
                courierId = session.courierId,
                status = session.status,
                location = session.latestLocation?.let(LocationDto::from),
                locationUpdatedAt = session.locationUpdatedAt?.toString(),
                createdAt = session.createdAt.toString(),
                updatedAt = session.updatedAt.toString(),
            )
    }
}

/** Frame pushed over the live WebSocket. */
@Serializable
data class TrackingUpdateMessage(
    val orderId: String,
    val status: DeliveryStatus,
    val location: LocationDto? = null,
    val timestamp: String,
) {
    companion object {
        fun from(update: TrackingUpdate): TrackingUpdateMessage =
            TrackingUpdateMessage(
                orderId = update.orderId,
                status = update.status,
                location = update.location?.let(LocationDto::from),
                timestamp = update.timestamp.toString(),
            )

        fun from(session: TrackingSession): TrackingUpdateMessage =
            TrackingUpdateMessage(
                orderId = session.orderId,
                status = session.status,
                location = session.latestLocation?.let(LocationDto::from),
                timestamp = session.updatedAt.toString(),
            )
    }
}
