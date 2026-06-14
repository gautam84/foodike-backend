package com.example.foodike.tracking.domain.port

import com.example.foodike.tracking.domain.model.TrackingSession

interface TrackingRepository {
    suspend fun findByOrderId(orderId: String): TrackingSession?
    suspend fun create(session: TrackingSession): TrackingSession
    suspend fun save(session: TrackingSession): TrackingSession
}
