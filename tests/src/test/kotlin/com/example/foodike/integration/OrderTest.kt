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

class OrderTest {

    @Test
    fun customerBuildsCartChecksOutAndStaffAdvancesStatus() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val otherCustomerToken = mintToken("other-${Random.nextLong()}", UserRole.CUSTOMER)

        // Restaurant A: deliveryFee 4000, minOrder 10000, item priced 29900
        val (restaurantA, itemA) = seedRestaurantWithItem(ownerToken, deliveryFee = 4000, minOrder = 10000, price = 29900)
        // Restaurant B: a different restaurant, to exercise the single-restaurant rule
        val (_, itemB) = seedRestaurantWithItem(ownerToken, deliveryFee = 2000, minOrder = 0, price = 15000)

        // Add item A to the cart
        val addA = client.post("/api/v1/cart/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"menuItemId":"$itemA","quantity":2}""")
        }
        assertEquals(HttpStatusCode.Created, addA.status, addA.bodyAsText())

        val cartAfterA = client.get("/api/v1/cart") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }.bodyAsText()
        assertEquals(restaurantA, extractJsonValue(cartAfterA, "restaurantId"))
        assertEquals(59800.0, numberField(cartAfterA, "subtotalAmount"), "2 x 29900 expected in $cartAfterA")

        // Adding an item from another restaurant replaces the cart (single-restaurant rule)
        val addB = client.post("/api/v1/cart/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"menuItemId":"$itemB","quantity":1}""")
        }
        assertEquals(HttpStatusCode.Created, addB.status, addB.bodyAsText())
        val cartAfterB = client.get("/api/v1/cart") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }.bodyAsText()
        assertEquals(15000.0, numberField(cartAfterB, "subtotalAmount"), "cart should now hold only item B: $cartAfterB")

        // Switch back to restaurant A and check out
        client.post("/api/v1/cart/items") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"menuItemId":"$itemA","quantity":1}""")
        }
        val checkout = client.post("/api/v1/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"addressId":"addr-1"}""")
        }
        assertEquals(HttpStatusCode.Created, checkout.status, checkout.bodyAsText())
        val orderBody = checkout.bodyAsText()
        val orderId = extractJsonValue(orderBody, "id")
        assertEquals(29900.0, numberField(orderBody, "subtotalAmount"), orderBody)
        assertEquals(4000.0, numberField(orderBody, "deliveryFeeAmount"), orderBody)
        assertEquals(33900.0, numberField(orderBody, "totalAmount"), "subtotal + deliveryFee expected: $orderBody")
        assertTrue(orderBody.contains("\"CREATED\""), "new order should be CREATED: $orderBody")

        // Cart is emptied after checkout
        val emptyCart = client.get("/api/v1/cart") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }.bodyAsText()
        assertEquals(0.0, numberField(emptyCart, "subtotalAmount"), "cart should be empty after checkout: $emptyCart")

        // Owner lists nothing for the customer; the customer sees their order
        val list = client.get("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }.bodyAsText()
        assertTrue(list.contains(orderId), "order should appear in customer's list: $list")

        // A different customer cannot read this order
        val forbiddenRead = client.get("/api/v1/orders/$orderId") {
            header(HttpHeaders.Authorization, "Bearer $otherCustomerToken")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenRead.status)

        // A customer cannot change order status
        val forbiddenStatus = client.patch("/api/v1/orders/$orderId/status") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $customerToken")
            setBody("""{"status":"CONFIRMED"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenStatus.status)

        // Illegal jump is rejected
        val illegal = client.patch("/api/v1/orders/$orderId/status") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            setBody("""{"status":"DELIVERED"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, illegal.status, illegal.bodyAsText())

        // Staff advances the order one step at a time
        assertEquals(HttpStatusCode.OK, transition(ownerToken, orderId, "CONFIRMED").status)
        val preparing = transition(ownerToken, orderId, "PREPARING")
        assertEquals(HttpStatusCode.OK, preparing.status, preparing.bodyAsText())
        assertTrue(preparing.bodyAsText().contains("\"PREPARING\""))
    }

    @Test
    fun checkoutBelowMinimumOrderIsRejected() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val (_, itemId) = seedRestaurantWithItem(ownerToken, deliveryFee = 0, minOrder = 100000, price = 5000)

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
        assertEquals(HttpStatusCode.BadRequest, checkout.status, checkout.bodyAsText())
    }

    @Test
    fun customerCancelsCreatedOrderButNotDelivered() = testApplication {
        environment {
            config = io.ktor.server.config.yaml.YamlConfig("application.yaml")
                ?: error("application.yaml not found on classpath")
        }

        val ownerToken = mintToken("owner-${Random.nextLong()}", UserRole.RESTAURANT_OWNER)
        val customerToken = mintToken("customer-${Random.nextLong()}", UserRole.CUSTOMER)
        val (_, itemId) = seedRestaurantWithItem(ownerToken, deliveryFee = 0, minOrder = 0, price = 12000)

        // First order: cancel it while CREATED
        val firstOrderId = placeOrder(customerToken, itemId)
        val cancel = client.post("/api/v1/orders/$firstOrderId/cancel") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }
        assertEquals(HttpStatusCode.OK, cancel.status, cancel.bodyAsText())
        assertTrue(cancel.bodyAsText().contains("\"CANCELLED\""))

        // Second order: drive it to DELIVERED, then cancelling must conflict
        val secondOrderId = placeOrder(customerToken, itemId)
        listOf("CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED").forEach { target ->
            assertEquals(HttpStatusCode.OK, transition(ownerToken, secondOrderId, target).status, "advancing to $target")
        }
        val lateCancel = client.post("/api/v1/orders/$secondOrderId/cancel") {
            header(HttpHeaders.Authorization, "Bearer $customerToken")
        }
        assertEquals(HttpStatusCode.Conflict, lateCancel.status, lateCancel.bodyAsText())
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

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.transition(
        ownerToken: String,
        orderId: String,
        status: String,
    ) = client.patch("/api/v1/orders/$orderId/status") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $ownerToken")
        setBody("""{"status":"$status"}""")
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
