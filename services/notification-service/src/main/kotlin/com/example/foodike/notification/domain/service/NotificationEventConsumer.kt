package com.example.foodike.notification.domain.service

import com.example.foodike.events.EventBus
import com.example.foodike.events.EventEnvelope
import com.example.foodike.notification.domain.model.NotificationChannel
import com.example.foodike.notification.domain.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * The codebase's first [EventBus] subscriber: it collects domain events and turns the ones that
 * carry an addressable user into persisted, dispatched notifications. Each event is handled in
 * isolation so a single malformed payload can never tear down the collector.
 */
class NotificationEventConsumer(
    private val eventBus: EventBus,
    private val service: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(NotificationEventConsumer::class.java)

    fun start(scope: CoroutineScope) {
        scope.launch {
            eventBus.subscribe().collect { envelope ->
                try {
                    handle(envelope)
                } catch (exception: Exception) {
                    logger.warn("Failed to process event {} ({})", envelope.type, envelope.eventId, exception)
                }
            }
        }
    }

    private suspend fun handle(envelope: EventEnvelope) {
        when (envelope.type) {
            "user.registered" -> {
                val userId = envelope.string("userId") ?: return
                service.notify(
                    userId = userId,
                    type = NotificationType.WELCOME,
                    channel = NotificationChannel.SMS,
                    title = "Welcome to Foodike",
                    body = "Your account is ready. Start exploring restaurants near you.",
                    recipient = envelope.string("phone"),
                )
            }

            "order.placed" -> {
                val userId = envelope.string("userId") ?: return
                val orderId = envelope.string("orderId")
                service.notify(
                    userId = userId,
                    type = NotificationType.ORDER_PLACED,
                    channel = NotificationChannel.PUSH,
                    title = "Order placed",
                    body = "We've received your order${orderId?.let { " #$it" } ?: ""}. You'll be notified as it progresses.",
                )
            }
            // payment.verified carries no userId yet — see NotificationEventConsumer plan note.
        }
    }

    private fun EventEnvelope.string(key: String): String? =
        payload[key]?.jsonPrimitive?.content
}
