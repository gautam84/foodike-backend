package com.example.foodike.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Money(
    val amount: Int,
    val currency: String = "INR",
)

@Serializable
data class Location(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class PageRequest(
    val page: Int = 1,
    val size: Int = 20,
    val sortBy: String? = null,
    val sortDir: SortDirection = SortDirection.ASC,
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalPages: Int,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

@Serializable
enum class SortDirection {
    ASC,
    DESC,
}
