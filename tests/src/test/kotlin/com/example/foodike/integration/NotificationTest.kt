package com.example.foodike.integration

import com.example.foodike.auth.JwtConfig
import com.example.foodike.auth.JwtService
import com.example.foodike.auth.UserPrincipal
import com.example.foodike.auth.UserRole
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val SENT_STATUS = Regex("\"status\"\\s*:\\s*\"SENT\"")

class NotificationTest {

    @Test
    fun placingAnOrderProducesAnOrderPlacedNotificationForThatCustomerOnly() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val otherCustomerToken = mintToken("other-${Random.nextLong()}", UserRole.CUSTOMER)

        val itemId = seedRestaurantWithItem(ownerToken, price = 12000)

        // Checkout publishes order.placed, which the notification consumer turns into a notification.
        client.post("/api/v1/cart/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"menuItemId":"$itemId","quantity":1}""")
        }
        val checkout = client.post("/api/v1/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{}""")
        }
        assertEquals(HttpStatusCode.Created, checkout.status, checkout.bodyAsText())

        // Event delivery and dispatch are async; poll until the notification reaches its SENT state.
        val notifications = awaitNotifications(customerToken) {
            it.contains("ORDER_PLACED") && SENT_STATUS.containsMatchIn(it)
        }
        assertTrue(notifications.contains("ORDER_PLACED"), "expected an ORDER_PLACED notification: $notifications")
        assertTrue(SENT_STATUS.containsMatchIn(notifications), "stub sender should mark it SENT: $notifications")

        // A different customer must not see this notification.
        val others = client.get("/api/v1/notifications") {
            header(HttpHeaders.Authorization, "Bearer $otherCustomerToken")
        }.bodyAsText()
        assertTrue(!others.contains("ORDER_PLACED"), "other customer should have no order notifications: $others")
    }

    private suspend fun ApplicationTestBuilder.awaitNotifications(
        token: String,
        predicate: (String) -> Boolean,
    ): String {
        repeat(20) {
            val body = client.get("/api/v1/notifications") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.bodyAsText()
            if (predicate(body)) return body
            delay(100)
        }
        return client.get("/api/v1/notifications") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.bodyAsText()
    }

    private suspend fun ApplicationTestBuilder.seedRestaurantWithItem(ownerToken: String, price: Int): String {
        val name = "Eatery ${Random.nextInt(1_000_000)}"
        val createResponse = client.post("/api/v1/restaurants") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody(
                """
                {"name":"$name","line1":"1 Road","city":"Bengaluru","state":"Karnataka",
                 "postalCode":"560001","country":"IN","deliveryFee":0,"minOrder":0}
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status, createResponse.bodyAsText())
        val restaurantId = extractJsonValue(createResponse.bodyAsText(), "id")

        val categoryResponse = client.post("/api/v1/restaurants/$restaurantId/categories") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"name":"Mains","sortOrder":1}""")
        }
        val categoryId = extractJsonValue(categoryResponse.bodyAsText(), "id")

        val itemResponse = client.post("/api/v1/categories/$categoryId/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"name":"Dish ${Random.nextInt(1_000_000)}","priceAmount":$price}""")
        }
        assertEquals(HttpStatusCode.Created, itemResponse.status, itemResponse.bodyAsText())
        return extractJsonValue(itemResponse.bodyAsText(), "id")
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
}
