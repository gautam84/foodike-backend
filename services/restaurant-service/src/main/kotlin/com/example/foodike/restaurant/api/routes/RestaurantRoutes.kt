package com.example.foodike.restaurant.api.routes

import com.example.foodike.auth.UserPrincipal
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.PageRequest
import com.example.foodike.common.model.PaginatedResponse
import com.example.foodike.restaurant.api.dto.CreateCategoryRequest
import com.example.foodike.restaurant.api.dto.CreateItemRequest
import com.example.foodike.restaurant.api.dto.CreateRestaurantRequest
import com.example.foodike.restaurant.api.dto.HourDto
import com.example.foodike.restaurant.api.dto.HoursResponse
import com.example.foodike.restaurant.api.dto.MenuCategoryResponse
import com.example.foodike.restaurant.api.dto.MenuItemResponse
import com.example.foodike.restaurant.api.dto.RestaurantDetailResponse
import com.example.foodike.restaurant.api.dto.RestaurantResponse
import com.example.foodike.restaurant.api.dto.ReviewListResponse
import com.example.foodike.restaurant.api.dto.ReviewResponse
import com.example.foodike.restaurant.api.dto.SetHoursRequest
import com.example.foodike.restaurant.api.dto.SubmitReviewRequest
import com.example.foodike.restaurant.api.dto.UpdateCategoryRequest
import com.example.foodike.restaurant.api.dto.UpdateItemRequest
import com.example.foodike.restaurant.api.dto.UpdateRestaurantRequest
import com.example.foodike.restaurant.domain.port.RestaurantFilter
import com.example.foodike.restaurant.domain.service.Actor
import com.example.foodike.restaurant.domain.service.CreateCategoryInput
import com.example.foodike.restaurant.domain.service.CreateItemInput
import com.example.foodike.restaurant.domain.service.CreateRestaurantInput
import com.example.foodike.restaurant.domain.service.HourInput
import com.example.foodike.restaurant.domain.service.MenuService
import com.example.foodike.restaurant.domain.service.RestaurantService
import com.example.foodike.restaurant.domain.service.ReviewService
import com.example.foodike.restaurant.domain.service.SubmitReviewInput
import com.example.foodike.restaurant.domain.service.UpdateCategoryInput
import com.example.foodike.restaurant.domain.service.UpdateItemInput
import com.example.foodike.restaurant.domain.service.UpdateRestaurantInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlin.math.ceil
import org.koin.ktor.ext.inject

fun Route.registerRestaurantRoutes() {
    val restaurantService by inject<RestaurantService>()
    val menuService by inject<MenuService>()
    val reviewService by inject<ReviewService>()

    route("/restaurants") {
        // Public catalog reads
        get {
            call.respond(searchRestaurants(restaurantService))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
            val restaurant = restaurantService.get(id)
            val hours = restaurantService.getHours(id)
            val menu = menuService.listMenu(id)
            call.respond(
                RestaurantDetailResponse(
                    restaurant = RestaurantResponse.from(restaurant),
                    hours = hours.map(HourDto::from),
                    menu = menu.map(MenuCategoryResponse::from),
                ),
            )
        }

        get("/{id}/reviews") {
            val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
            val reviews = reviewService.list(id).map(ReviewResponse::from)
            call.respond(ReviewListResponse(reviews))
        }

        authenticate("auth-jwt") {
            post {
                val actor = call.actor()
                val request = call.receive<CreateRestaurantRequest>()
                val created = restaurantService.create(actor, request.toInput())
                call.respond(HttpStatusCode.Created, RestaurantResponse.from(created))
            }

            patch("/{id}") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
                val request = call.receive<UpdateRestaurantRequest>()
                val updated = restaurantService.update(actor, id, request.toInput())
                call.respond(RestaurantResponse.from(updated))
            }

            delete("/{id}") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
                restaurantService.delete(actor, id)
                call.respond(HttpStatusCode.NoContent)
            }

            put("/{id}/hours") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
                val request = call.receive<SetHoursRequest>()
                val hours = restaurantService.setHours(
                    actor,
                    id,
                    request.hours.map { HourInput(it.dayOfWeek, it.opensAt, it.closesAt) },
                )
                call.respond(HoursResponse(hours.map(HourDto::from)))
            }

            post("/{id}/categories") {
                val actor = call.actor()
                val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
                val request = call.receive<CreateCategoryRequest>()
                val created = menuService.createCategory(
                    actor,
                    id,
                    CreateCategoryInput(name = request.name, sortOrder = request.sortOrder),
                )
                call.respond(HttpStatusCode.Created, MenuCategoryResponse.from(created))
            }

            post("/{id}/reviews") {
                val principal = call.requirePrincipal()
                val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
                val request = call.receive<SubmitReviewRequest>()
                val review = reviewService.submit(
                    userId = principal.userId,
                    restaurantId = id,
                    input = SubmitReviewInput(rating = request.rating, comment = request.comment),
                )
                call.respond(ReviewResponse.from(review))
            }

            delete("/{id}/reviews") {
                val principal = call.requirePrincipal()
                val id = call.parameters["id"] ?: throw ValidationException("Restaurant id required")
                reviewService.delete(userId = principal.userId, restaurantId = id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    // Public search endpoint (alias of GET /restaurants)
    route("/search") {
        get {
            call.respond(searchRestaurants(restaurantService))
        }
    }

    authenticate("auth-jwt") {
        route("/categories/{categoryId}") {
            patch {
                val actor = call.actor()
                val categoryId = call.parameters["categoryId"] ?: throw ValidationException("Category id required")
                val request = call.receive<UpdateCategoryRequest>()
                val updated = menuService.updateCategory(
                    actor,
                    categoryId,
                    UpdateCategoryInput(name = request.name, sortOrder = request.sortOrder),
                )
                call.respond(MenuCategoryResponse.from(updated))
            }

            delete {
                val actor = call.actor()
                val categoryId = call.parameters["categoryId"] ?: throw ValidationException("Category id required")
                menuService.deleteCategory(actor, categoryId)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/items") {
                val actor = call.actor()
                val categoryId = call.parameters["categoryId"] ?: throw ValidationException("Category id required")
                val request = call.receive<CreateItemRequest>()
                val created = menuService.createItem(actor, categoryId, request.toInput())
                call.respond(HttpStatusCode.Created, MenuItemResponse.from(created))
            }
        }

        route("/items/{itemId}") {
            patch {
                val actor = call.actor()
                val itemId = call.parameters["itemId"] ?: throw ValidationException("Item id required")
                val request = call.receive<UpdateItemRequest>()
                val updated = menuService.updateItem(actor, itemId, request.toInput())
                call.respond(MenuItemResponse.from(updated))
            }

            delete {
                val actor = call.actor()
                val itemId = call.parameters["itemId"] ?: throw ValidationException("Item id required")
                menuService.deleteItem(actor, itemId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private suspend fun RoutingContext.searchRestaurants(
    restaurantService: RestaurantService,
): PaginatedResponse<RestaurantResponse> {
    val params = call.request.queryParameters
    val page = params["page"]?.toIntOrNull() ?: 1
    val size = params["size"]?.toIntOrNull() ?: 20
    val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: true
    val filter = RestaurantFilter(
        query = params["q"],
        cuisine = params["cuisine"],
        city = params["city"],
        enabled = enabled,
    )
    val result = restaurantService.search(filter, PageRequest(page = page, size = size))
    val effectiveSize = size.coerceIn(1, 100)
    val totalPages = if (result.total == 0L) 0 else ceil(result.total.toDouble() / effectiveSize).toInt()
    return PaginatedResponse(
        data = result.items.map(RestaurantResponse::from),
        page = page.coerceAtLeast(1),
        size = effectiveSize,
        totalPages = totalPages,
    )
}

private fun ApplicationCall.requirePrincipal(): UserPrincipal =
    principal<UserPrincipal>() ?: throw UnauthorizedException()

private fun ApplicationCall.actor(): Actor {
    val principal = requirePrincipal()
    return Actor(userId = principal.userId, role = principal.role)
}

private fun CreateRestaurantRequest.toInput() = CreateRestaurantInput(
    name = name,
    description = description,
    cuisines = cuisines,
    phone = phone,
    line1 = line1,
    city = city,
    state = state,
    postalCode = postalCode,
    country = country,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
    deliveryFee = deliveryFee,
    minOrder = minOrder,
    prepTimeMins = prepTimeMins,
    enabled = enabled,
)

private fun UpdateRestaurantRequest.toInput() = UpdateRestaurantInput(
    name = name,
    description = description,
    cuisines = cuisines,
    phone = phone,
    line1 = line1,
    city = city,
    state = state,
    postalCode = postalCode,
    country = country,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
    deliveryFee = deliveryFee,
    minOrder = minOrder,
    prepTimeMins = prepTimeMins,
    enabled = enabled,
)

private fun CreateItemRequest.toInput() = CreateItemInput(
    name = name,
    description = description,
    priceAmount = priceAmount,
    currency = currency,
    isVeg = isVeg,
    isAvailable = isAvailable,
    imageUrl = imageUrl,
)

private fun UpdateItemRequest.toInput() = UpdateItemInput(
    name = name,
    description = description,
    priceAmount = priceAmount,
    currency = currency,
    isVeg = isVeg,
    isAvailable = isAvailable,
    imageUrl = imageUrl,
)
