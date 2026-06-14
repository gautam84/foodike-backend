package com.example.foodike.tracking.domain.service

import com.example.foodike.events.EventBus
import com.example.foodike.events.EventEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Subscribes to the [EventBus] and opens a tracking session whenever an order is placed, so a
 * customer can begin following their delivery the moment it is created. Each event is handled in
 * isolation so a single malformed payload can never tear down the collector.
 */
class TrackingEventConsumer(
    private val eventBus: EventBus,
    private val service: TrackingService,
) {
    private val logger = LoggerFactory.getLogger(TrackingEventConsumer::class.java)

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
            "order.placed" -> {
                val orderId = envelope.string("orderId") ?: return
                val userId = envelope.string("userId") ?: return
                service.createSession(orderId = orderId, customerId = userId)
            }
        }
    }

    private fun EventEnvelope.string(key: String): String? =
        payload[key]?.jsonPrimitive?.content
}
