package com.example.foodike.plugins

import com.example.foodike.tracking.domain.service.TrackingEventConsumer
import io.ktor.server.application.Application
import org.koin.core.context.GlobalContext

/**
 * Activates the tracking-service EventBus subscriber, which opens a tracking session whenever an
 * order is placed. Must run after [configureInfrastructure] has installed Koin. The Ktor
 * [Application] is itself a CoroutineScope, so the consumer collects for the lifetime of the server.
 */
fun Application.startTrackingConsumer() {
    val consumer = GlobalContext.get().get<TrackingEventConsumer>()
    consumer.start(this)
}
