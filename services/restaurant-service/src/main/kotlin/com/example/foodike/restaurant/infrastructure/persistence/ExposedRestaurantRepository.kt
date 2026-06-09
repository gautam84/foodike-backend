package com.example.foodike.restaurant.infrastructure.persistence

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.persistence.dbQuery
import com.example.foodike.restaurant.domain.model.Restaurant
import com.example.foodike.restaurant.domain.model.RestaurantHour
import com.example.foodike.restaurant.domain.port.RestaurantFilter
import com.example.foodike.restaurant.domain.port.RestaurantPage
import com.example.foodike.restaurant.domain.port.RestaurantRepository
import java.time.Instant
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedRestaurantRepository : RestaurantRepository {
    override suspend fun create(restaurant: Restaurant): Restaurant =
        dbQuery {
            RestaurantsTable.insert { it.fromRestaurant(restaurant) }
            restaurant
        }

    override suspend fun update(restaurant: Restaurant): Restaurant =
        dbQuery {
            val rows = RestaurantsTable.update({ RestaurantsTable.id eq restaurant.id }) {
                it.fromRestaurant(restaurant, includeRating = false)
            }
            if (rows == 0) throw NotFoundException("Restaurant not found")
            restaurant
        }

    override suspend fun findById(id: String): Restaurant? =
        dbQuery {
            RestaurantsTable
                .selectAll()
                .where { RestaurantsTable.id eq id }
                .singleOrNull()
                ?.toRestaurant()
        }

    override suspend fun delete(id: String): Boolean =
        dbQuery {
            RestaurantsTable.deleteWhere { RestaurantsTable.id eq id } > 0
        }

    override suspend fun search(filter: RestaurantFilter, page: PageRequest): RestaurantPage =
        dbQuery {
            val predicate = buildPredicate(filter)
            val total = RestaurantsTable.selectAll().where { predicate }.count()
            val offset = ((page.page - 1).coerceAtLeast(0).toLong()) * page.size
            val items = RestaurantsTable
                .selectAll()
                .where { predicate }
                .orderBy(RestaurantsTable.rating to SortOrder.DESC, RestaurantsTable.name to SortOrder.ASC)
                .limit(page.size)
                .offset(offset)
                .map { it.toRestaurant() }
            RestaurantPage(items = items, total = total)
        }

    override suspend fun replaceHours(restaurantId: String, hours: List<RestaurantHour>): List<RestaurantHour> =
        dbQuery {
            RestaurantHoursTable.deleteWhere { RestaurantHoursTable.restaurantId eq restaurantId }
            hours.forEach { hour ->
                RestaurantHoursTable.insert {
                    it[id] = hour.id
                    it[RestaurantHoursTable.restaurantId] = restaurantId
                    it[dayOfWeek] = hour.dayOfWeek
                    it[opensAt] = hour.opensAt
                    it[closesAt] = hour.closesAt
                }
            }
            hours
        }

    override suspend fun findHours(restaurantId: String): List<RestaurantHour> =
        dbQuery {
            RestaurantHoursTable
                .selectAll()
                .where { RestaurantHoursTable.restaurantId eq restaurantId }
                .orderBy(RestaurantHoursTable.dayOfWeek to SortOrder.ASC)
                .map { it.toHour() }
        }

    override suspend fun recomputeRating(restaurantId: String): Restaurant? =
        dbQuery {
            val avgExpr = ReviewsTable.rating.avg()
            val countExpr = ReviewsTable.id.count()
            val row = ReviewsTable
                .select(avgExpr, countExpr)
                .where { ReviewsTable.restaurantId eq restaurantId }
                .singleOrNull()
            val count = row?.get(countExpr) ?: 0L
            val avg = row?.get(avgExpr)?.toDouble() ?: 0.0
            RestaurantsTable.update({ RestaurantsTable.id eq restaurantId }) {
                it[rating] = avg
                it[ratingCount] = count.toInt()
                it[updatedAt] = Instant.now()
            }
            RestaurantsTable
                .selectAll()
                .where { RestaurantsTable.id eq restaurantId }
                .singleOrNull()
                ?.toRestaurant()
        }

    private fun buildPredicate(filter: RestaurantFilter): Op<Boolean> {
        var predicate: Op<Boolean> = Op.TRUE
        filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let { q ->
            predicate = predicate and (RestaurantsTable.name.lowerCase() like "%${q.lowercase()}%")
        }
        filter.cuisine?.trim()?.takeIf { it.isNotEmpty() }?.let { c ->
            predicate = predicate and (RestaurantsTable.cuisines like "%${c.lowercase()}%")
        }
        filter.city?.trim()?.takeIf { it.isNotEmpty() }?.let { city ->
            predicate = predicate and (RestaurantsTable.city.lowerCase() eq city.lowercase())
        }
        filter.enabled?.let { enabled ->
            predicate = predicate and (RestaurantsTable.enabled eq enabled)
        }
        return predicate
    }
}

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromRestaurant(
    restaurant: Restaurant,
    includeRating: Boolean = true,
) {
    this[RestaurantsTable.id] = restaurant.id
    this[RestaurantsTable.ownerId] = restaurant.ownerId
    this[RestaurantsTable.name] = restaurant.name
    this[RestaurantsTable.description] = restaurant.description
    this[RestaurantsTable.cuisines] = restaurant.cuisines.joinToString(",")
    this[RestaurantsTable.phone] = restaurant.phone
    this[RestaurantsTable.line1] = restaurant.line1
    this[RestaurantsTable.city] = restaurant.city
    this[RestaurantsTable.state] = restaurant.state
    this[RestaurantsTable.postalCode] = restaurant.postalCode
    this[RestaurantsTable.country] = restaurant.country
    this[RestaurantsTable.latitude] = restaurant.latitude
    this[RestaurantsTable.longitude] = restaurant.longitude
    this[RestaurantsTable.imageUrl] = restaurant.imageUrl
    this[RestaurantsTable.deliveryFee] = restaurant.deliveryFee
    this[RestaurantsTable.minOrder] = restaurant.minOrder
    this[RestaurantsTable.prepTimeMins] = restaurant.prepTimeMins
    this[RestaurantsTable.enabled] = restaurant.enabled
    this[RestaurantsTable.updatedAt] = restaurant.updatedAt
    if (includeRating) {
        this[RestaurantsTable.rating] = restaurant.rating
        this[RestaurantsTable.ratingCount] = restaurant.ratingCount
        this[RestaurantsTable.createdAt] = restaurant.createdAt
    }
}

private fun ResultRow.toRestaurant() =
    Restaurant(
        id = this[RestaurantsTable.id],
        ownerId = this[RestaurantsTable.ownerId],
        name = this[RestaurantsTable.name],
        description = this[RestaurantsTable.description],
        cuisines = this[RestaurantsTable.cuisines].split(",").map { it.trim() }.filter { it.isNotEmpty() },
        phone = this[RestaurantsTable.phone],
        line1 = this[RestaurantsTable.line1],
        city = this[RestaurantsTable.city],
        state = this[RestaurantsTable.state],
        postalCode = this[RestaurantsTable.postalCode],
        country = this[RestaurantsTable.country],
        latitude = this[RestaurantsTable.latitude],
        longitude = this[RestaurantsTable.longitude],
        imageUrl = this[RestaurantsTable.imageUrl],
        rating = this[RestaurantsTable.rating],
        ratingCount = this[RestaurantsTable.ratingCount],
        deliveryFee = this[RestaurantsTable.deliveryFee],
        minOrder = this[RestaurantsTable.minOrder],
        prepTimeMins = this[RestaurantsTable.prepTimeMins],
        enabled = this[RestaurantsTable.enabled],
        createdAt = this[RestaurantsTable.createdAt],
        updatedAt = this[RestaurantsTable.updatedAt],
    )

private fun ResultRow.toHour() =
    RestaurantHour(
        id = this[RestaurantHoursTable.id],
        restaurantId = this[RestaurantHoursTable.restaurantId],
        dayOfWeek = this[RestaurantHoursTable.dayOfWeek],
        opensAt = this[RestaurantHoursTable.opensAt],
        closesAt = this[RestaurantHoursTable.closesAt],
    )
