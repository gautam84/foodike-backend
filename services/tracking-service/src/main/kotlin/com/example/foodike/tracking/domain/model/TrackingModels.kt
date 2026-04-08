package com.example.foodike.tracking.domain.model

import com.example.foodike.common.model.Location
import kotlinx.serialization.Serializable

@Serializable
data class TrackingSession(
    val orderId: String,
    val courierId: String,
    val latestLocation: Location? = null,
)
