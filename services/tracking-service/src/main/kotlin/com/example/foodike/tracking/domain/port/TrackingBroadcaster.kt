package com.example.foodike.tracking.domain.port

import com.example.foodike.tracking.domain.model.TrackingUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Live fan-out of delivery updates to WebSocket subscribers. The in-process implementation is
 * SharedFlow-backed; designed to swap for a broker-backed implementation on microservice extraction.
 */
interface TrackingBroadcaster {
    suspend fun publish(orderId: String, update: TrackingUpdate)
    fun stream(orderId: String): Flow<TrackingUpdate>
}
