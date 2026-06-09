package com.example.foodike.restaurant.domain.service

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.restaurant.domain.model.Review
import com.example.foodike.restaurant.domain.port.RestaurantRepository
import com.example.foodike.restaurant.domain.port.ReviewRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class SubmitReviewInput(
    val rating: Int,
    val comment: String?,
)

class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val restaurantRepository: RestaurantRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun list(restaurantId: String): List<Review> {
        restaurantRepository.findById(restaurantId) ?: throw NotFoundException("Restaurant not found")
        return reviewRepository.findByRestaurant(restaurantId)
    }

    suspend fun submit(userId: String, restaurantId: String, input: SubmitReviewInput): Review {
        restaurantRepository.findById(restaurantId) ?: throw NotFoundException("Restaurant not found")
        if (input.rating !in 1..5) {
            throw ValidationException("Review is not valid", mapOf("rating" to "Must be 1-5"))
        }
        val comment = input.comment?.trim()?.ifEmpty { null }
        if (comment != null && comment.length > 1000) {
            throw ValidationException("Review is not valid", mapOf("comment" to "Maximum length is 1000"))
        }

        val now = Instant.now(clock)
        val saved = reviewRepository.upsert(
            Review(
                id = UUID.randomUUID().toString(),
                restaurantId = restaurantId,
                userId = userId,
                rating = input.rating,
                comment = comment,
                createdAt = now,
                updatedAt = now,
            ),
        )
        restaurantRepository.recomputeRating(restaurantId)
        return saved
    }

    suspend fun delete(userId: String, restaurantId: String) {
        val deleted = reviewRepository.delete(restaurantId, userId)
        if (!deleted) throw NotFoundException("Review not found")
        restaurantRepository.recomputeRating(restaurantId)
    }
}
