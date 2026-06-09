package com.example.foodike.restaurant.api.dto

import com.example.foodike.restaurant.domain.model.Review
import kotlinx.serialization.Serializable

@Serializable
data class SubmitReviewRequest(
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class ReviewResponse(
    val id: String,
    val restaurantId: String,
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(review: Review): ReviewResponse =
            ReviewResponse(
                id = review.id,
                restaurantId = review.restaurantId,
                userId = review.userId,
                rating = review.rating,
                comment = review.comment,
                createdAt = review.createdAt.toString(),
                updatedAt = review.updatedAt.toString(),
            )
    }
}

@Serializable
data class ReviewListResponse(
    val reviews: List<ReviewResponse>,
)
