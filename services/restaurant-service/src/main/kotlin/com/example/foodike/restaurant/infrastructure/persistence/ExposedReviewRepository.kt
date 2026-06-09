package com.example.foodike.restaurant.infrastructure.persistence

import com.example.foodike.persistence.dbQuery
import com.example.foodike.restaurant.domain.model.Review
import com.example.foodike.restaurant.domain.port.ReviewRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedReviewRepository : ReviewRepository {
    override suspend fun upsert(review: Review): Review =
        dbQuery {
            val existing = ReviewsTable
                .selectAll()
                .where { (ReviewsTable.restaurantId eq review.restaurantId) and (ReviewsTable.userId eq review.userId) }
                .singleOrNull()

            if (existing == null) {
                ReviewsTable.insert {
                    it[id] = review.id
                    it[restaurantId] = review.restaurantId
                    it[userId] = review.userId
                    it[rating] = review.rating
                    it[comment] = review.comment
                    it[createdAt] = review.createdAt
                    it[updatedAt] = review.updatedAt
                }
                review
            } else {
                val existingId = existing[ReviewsTable.id]
                ReviewsTable.update({ ReviewsTable.id eq existingId }) {
                    it[rating] = review.rating
                    it[comment] = review.comment
                    it[updatedAt] = review.updatedAt
                }
                review.copy(id = existingId, createdAt = existing[ReviewsTable.createdAt])
            }
        }

    override suspend fun findByRestaurant(restaurantId: String): List<Review> =
        dbQuery {
            ReviewsTable
                .selectAll()
                .where { ReviewsTable.restaurantId eq restaurantId }
                .orderBy(ReviewsTable.createdAt to SortOrder.DESC)
                .map { it.toReview() }
        }

    override suspend fun findByUser(restaurantId: String, userId: String): Review? =
        dbQuery {
            ReviewsTable
                .selectAll()
                .where { (ReviewsTable.restaurantId eq restaurantId) and (ReviewsTable.userId eq userId) }
                .singleOrNull()
                ?.toReview()
        }

    override suspend fun delete(restaurantId: String, userId: String): Boolean =
        dbQuery {
            ReviewsTable.deleteWhere {
                (ReviewsTable.restaurantId eq restaurantId) and (ReviewsTable.userId eq userId)
            } > 0
        }
}

private fun ResultRow.toReview() =
    Review(
        id = this[ReviewsTable.id],
        restaurantId = this[ReviewsTable.restaurantId],
        userId = this[ReviewsTable.userId],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAt = this[ReviewsTable.createdAt],
        updatedAt = this[ReviewsTable.updatedAt],
    )
