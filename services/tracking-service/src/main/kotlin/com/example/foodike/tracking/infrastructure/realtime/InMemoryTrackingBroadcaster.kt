package com.example.foodike.tracking.infrastructure.realtime

import com.example.foodike.tracking.domain.model.TrackingUpdate
import com.example.foodike.tracking.domain.port.TrackingBroadcaster
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process live fan-out: one hot [MutableSharedFlow] per order, lazily created. Mirrors
 * `InProcessEventBus`; swap for a broker-backed implementation on microservice extraction.
 */
class InMemoryTrackingBroadcaster : TrackingBroadcaster {
    private val flows = ConcurrentHashMap<String, MutableSharedFlow<TrackingUpdate>>()

    private fun flowFor(orderId: String): MutableSharedFlow<TrackingUpdate> =
        flows.getOrPut(orderId) { MutableSharedFlow(extraBufferCapacity = 64) }

    override suspend fun publish(orderId: String, update: TrackingUpdate) {
        flowFor(orderId).emit(update)
    }

    override fun stream(orderId: String): Flow<TrackingUpdate> = flowFor(orderId).asSharedFlow()
}
