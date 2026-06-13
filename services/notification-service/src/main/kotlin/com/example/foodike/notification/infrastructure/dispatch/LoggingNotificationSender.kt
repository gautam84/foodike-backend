package com.example.foodike.notification.infrastructure.dispatch

import com.example.foodike.notification.domain.model.Notification
import com.example.foodike.notification.domain.port.NotificationSender
import org.slf4j.LoggerFactory

/**
 * Log-only stub dispatcher, analogous to the console OTP provider. It records the delivery and
 * reports success without contacting a real PUSH/SMS/EMAIL provider.
 */
class LoggingNotificationSender : NotificationSender {
    private val logger = LoggerFactory.getLogger(LoggingNotificationSender::class.java)

    override suspend fun send(notification: Notification): Boolean {
        logger.info(
            "Dispatching {} notification to user={} recipient={}: {}",
            notification.channel,
            notification.userId,
            notification.recipient ?: "-",
            notification.title,
        )
        return true
    }
}
