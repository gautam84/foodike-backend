package com.example.foodike.plugins

import com.example.foodike.notification.domain.service.NotificationEventConsumer
import io.ktor.server.application.Application
import org.koin.core.context.GlobalContext

/**
 * Activates the notification-service EventBus subscriber. Must run after [configureInfrastructure]
 * has installed Koin. The Ktor [Application] is itself a CoroutineScope, so the consumer collects
 * for the lifetime of the server.
 */
fun Application.startNotificationConsumer() {
    val consumer = GlobalContext.get().get<NotificationEventConsumer>()
    consumer.start(this)
}
