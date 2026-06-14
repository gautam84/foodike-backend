package com.example.foodike.events

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class EventEnvelope(
    val eventId: String = UUID.randomUUID().toString(),
    val type: String,
    val correlationId: String? = null,
    val timestamp: String = Instant.now().toString(),
    val payload: JsonObject = buildJsonObject { },
)

interface EventBus {
    suspend fun publish(event: EventEnvelope)
    fun subscribe(): SharedFlow<EventEnvelope>
}

class InProcessEventBus : EventBus {
    private val flow = MutableSharedFlow<EventEnvelope>(extraBufferCapacity = 64)

    override suspend fun publish(event: EventEnvelope) {
        flow.emit(event)
    }

    override fun subscribe(): SharedFlow<EventEnvelope> = flow
}

class AsyncEventPublisher(
    private val eventBus: EventBus,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    fun publish(type: String, payload: JsonObject = buildJsonObject { }) {
        scope.launch {
            eventBus.publish(EventEnvelope(type = type, payload = payload))
        }
    }
}

object UserEvents {
    fun userRegistered(userId: String, phone: String) = EventEnvelope(
        type = "user.registered",
        payload = buildJsonObject {
            put("userId", userId)
            put("phone", phone)
        },
    )
}

object RestaurantEvents {
    fun restaurantToggled(restaurantId: String, enabled: Boolean) = EventEnvelope(
        type = "restaurant.toggled",
        payload = buildJsonObject {
            put("restaurantId", restaurantId)
            put("enabled", enabled)
        },
    )
}

object OrderEvents {
    fun orderPlaced(orderId: String, userId: String) = EventEnvelope(
        type = "order.placed",
        payload = buildJsonObject {
            put("orderId", orderId)
            put("userId", userId)
        },
    )
}

object PaymentEvents {
    fun paymentVerified(paymentId: String, orderId: String, userId: String) = EventEnvelope(
        type = "payment.verified",
        payload = buildJsonObject {
            put("paymentId", paymentId)
            put("orderId", orderId)
            put("userId", userId)
        },
    )
}

object NotificationEvents {
    fun notificationRequested(channel: String, recipient: String) = EventEnvelope(
        type = "notification.requested",
        payload = buildJsonObject {
            put("channel", channel)
            put("recipient", recipient)
        },
    )
}

object TrackingEvents {
    fun deliveryStatusChanged(orderId: String, status: String, userId: String) = EventEnvelope(
        type = "delivery.status_changed",
        payload = buildJsonObject {
            put("orderId", orderId)
            put("status", status)
            put("userId", userId)
        },
    )
}
