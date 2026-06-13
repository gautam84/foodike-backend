package com.example.foodike.notification.di

import com.example.foodike.notification.domain.port.NotificationRepository
import com.example.foodike.notification.domain.port.NotificationSender
import com.example.foodike.notification.domain.service.NotificationEventConsumer
import com.example.foodike.notification.domain.service.NotificationService
import com.example.foodike.notification.infrastructure.dispatch.LoggingNotificationSender
import com.example.foodike.notification.infrastructure.persistence.ExposedNotificationRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val notificationModule: Module = module {
    single<NotificationRepository> { ExposedNotificationRepository() }
    single<NotificationSender> { LoggingNotificationSender() }
    single { NotificationService(repository = get(), sender = get()) }
    single { NotificationEventConsumer(eventBus = get(), service = get()) }
}
