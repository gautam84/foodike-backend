package com.example.foodike.integration

import com.example.foodike.auth.JwtConfig
import com.example.foodike.auth.JwtService
import com.example.foodike.auth.UserPrincipal
import com.example.foodike.auth.UserRole
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestaurantTest {

    @Test
    fun ownerCreatesRestaurantMenuAndReviewsDriveRating() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerId = "owner-${Random.nextLong()}"
        val ownerToken = mintToken(ownerId, UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val uniqueName = "Pizza Palace ${Random.nextInt(1_000_000)}"

        // Owner can create
        val createResponse = client.post("/api/v1/restaurants") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody(
                """
                {"name":"$uniqueName","description":"Wood-fired pizzas","cuisines":["italian","pizza"],
                 "line1":"12 MG Road","city":"Bengaluru","state":"Karnataka","postalCode":"560001","country":"IN"}
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status, createResponse.bodyAsText())
        val restaurantId = extractJsonValue(createResponse.bodyAsText(), "id")

        // Customer cannot create
        val forbiddenCreate = client.post("/api/v1/restaurants") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody(
                """{"name":"Nope","line1":"x","city":"y","state":"z","postalCode":"560001","country":"IN"}""",
            )
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenCreate.status)

        // Public list/search finds it
        val searchResponse = client.get("/api/v1/restaurants?q=${"Pizza Palace".replace(" ", "%20")}&city=bengaluru")
        assertEquals(HttpStatusCode.OK, searchResponse.status)
        val searchBody = searchResponse.bodyAsText()
        assertTrue(searchBody.contains(uniqueName), "expected $uniqueName in $searchBody")
        assertTrue(searchBody.contains("\"totalPages\""), "expected paginated response in $searchBody")

        // Owner adds a menu category + item
        val categoryResponse = client.post("/api/v1/restaurants/$restaurantId/categories") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"name":"Mains","sortOrder":1}""")
        }
        assertEquals(HttpStatusCode.Created, categoryResponse.status, categoryResponse.bodyAsText())
        val categoryId = extractJsonValue(categoryResponse.bodyAsText(), "id")

        val itemResponse = client.post("/api/v1/categories/$categoryId/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"name":"Margherita","priceAmount":29900,"isVeg":true}""")
        }
        assertEquals(HttpStatusCode.Created, itemResponse.status, itemResponse.bodyAsText())

        // Detail view includes the menu
        val detailResponse = client.get("/api/v1/restaurants/$restaurantId")
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        val detailBody = detailResponse.bodyAsText()
        assertTrue(detailBody.contains("Margherita"), "expected menu item in $detailBody")
        assertTrue(detailBody.contains("Mains"), "expected category in $detailBody")

        // A customer reviews -> rating recomputed
        val reviewResponse = client.post("/api/v1/restaurants/$restaurantId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"rating":5,"comment":"Excellent!"}""")
        }
        assertEquals(HttpStatusCode.OK, reviewResponse.status, reviewResponse.bodyAsText())

        val afterReview = client.get("/api/v1/restaurants/$restaurantId").bodyAsText()
        assertEquals(5.0, numberField(afterReview, "rating"), "rating should be 5.0 in $afterReview")
        assertEquals(1.0, numberField(afterReview, "ratingCount"), "ratingCount should be 1 in $afterReview")

        // Non-owner cannot edit
        val forbiddenPatch = client.patch("/api/v1/restaurants/$restaurantId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"name":"Hacked"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenPatch.status)
    }

    private fun mintToken(userId: String, role: UserRole): String {
        val jwtService = JwtService(
            JwtConfig(
                secret = "local-dev-secret",
                issuer = "foodike",
                audience = "foodike-clients",
            ),
        )
        return jwtService.generateAccessToken(UserPrincipal(userId = userId, role = role))
    }

    private fun extractJsonValue(json: String, key: String): String =
        Regex(""""$key"\s*:\s*"([^"]+)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?: error("Missing key '$key' in response: $json")

    private fun numberField(json: String, key: String): Double =
        Regex("\"$key\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.toDouble()
            ?: error("Missing numeric key '$key' in response: $json")
}
