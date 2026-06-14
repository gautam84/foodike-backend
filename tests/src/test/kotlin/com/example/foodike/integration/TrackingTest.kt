package com.example.foodike.integration

import com.example.foodike.auth.JwtConfig
import com.example.foodike.auth.JwtService
import com.example.foodike.auth.UserPrincipal
import com.example.foodike.auth.UserRole
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackingTest {

    @Test
    fun courierStreamsLocationAndCustomerFollowsLive() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val courierToken = mintToken("courier-${Random.nextLong()}", UserRole.DELIVERY)
        val otherCustomerToken = mintToken("other-${Random.nextLong()}", UserRole.CUSTOMER)

        val (_, itemId) = seedRestaurantWithItem(ownerToken, deliveryFee = 0, minOrder = 0, price = 12000)
        val orderId = placeOrder(customerToken, itemId)

        // The order.placed event opens a tracking session; getSession also creates one lazily.
        val initial = client.get("/api/v1/track/$orderId") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }
        assertEquals(HttpStatusCode.OK, initial.status, initial.bodyAsText())
        assertTrue(initial.bodyAsText().contains("\"PENDING_ASSIGNMENT\""), initial.bodyAsText())

        // A courier cannot push location before assignment (session not yet active).
        val tooEarly = pushLocation(courierToken, orderId, lat = 12.90, lng = 77.50)
        assertEquals(HttpStatusCode.Conflict, tooEarly.status, tooEarly.bodyAsText())

        // Staff assigns a courier, moving the delivery to ASSIGNED.
        val assign = client.post("/api/v1/track/$orderId/courier") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $courierToken")
            setBody("""{"courierId":"courier-7"}""")
        }
        assertEquals(HttpStatusCode.OK, assign.status, assign.bodyAsText())
        assertTrue(assign.bodyAsText().contains("\"ASSIGNED\""), assign.bodyAsText())

        // Courier pushes a position; it lands on the session snapshot.
        val push = pushLocation(courierToken, orderId, lat = 12.9716, lng = 77.5946)
        assertEquals(HttpStatusCode.OK, push.status, push.bodyAsText())
        val snapshot = client.get("/api/v1/track/$orderId") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }.bodyAsText()
        assertEquals(12.9716, numberField(snapshot, "lat"), snapshot)
        assertEquals(77.5946, numberField(snapshot, "lng"), snapshot)

        // A different customer cannot view this delivery.
        val forbidden = client.get("/api/v1/track/$orderId") {
            header(HttpHeaders.Authorization, "Bearer $otherCustomerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, forbidden.status)

        // Live WebSocket: the first frame is the current snapshot; a subsequent courier push
        // arrives as a live frame.
        val wsClient = createClient { install(WebSockets) }
        wsClient.webSocket(
            urlString = "/api/v1/track/$orderId/live",
            request = { header(HttpHeaders.Authorization, "Bearer $customerToken") },
        ) {
            val first = (incoming.receive() as Frame.Text).readText()
            assertTrue(first.contains("\"ASSIGNED\""), "snapshot frame: $first")

            // Give the server collector time to subscribe, then push a new position.
            delay(300)
            val live = pushLocation(courierToken, orderId, lat = 13.0827, lng = 80.2707)
            assertEquals(HttpStatusCode.OK, live.status, live.bodyAsText())

            val frame = withTimeout(5_000) { (incoming.receive() as Frame.Text).readText() }
            assertEquals(13.0827, numberField(frame, "lat"), "live frame: $frame")
        }
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.pushLocation(
        token: String,
        orderId: String,
        lat: Double,
        lng: Double,
    ) = client.post("/api/v1/track/$orderId/location") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $token")
        setBody("""{"lat":$lat,"lng":$lng}""")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.seedRestaurantWithItem(
        ownerToken: String,
        deliveryFee: Int,
        minOrder: Int,
        price: Int,
    ): Pair<String, String> {
        val name = "Eatery ${Random.nextInt(1_000_000)}"
        val createResponse = client.post("/api/v1/restaurants") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody(
                """
                {"name":"$name","line1":"1 Road","city":"Bengaluru","state":"Karnataka",
                 "postalCode":"560001","country":"IN","deliveryFee":$deliveryFee,"minOrder":$minOrder}
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
        return restaurantId to extractJsonValue(itemResponse.bodyAsText(), "id")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.placeOrder(
        customerToken: String,
        itemId: String,
    ): String {
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
        return extractJsonValue(checkout.bodyAsText(), "id")
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
