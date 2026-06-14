package com.example.foodike.tracking.di

import com.example.foodike.tracking.domain.port.TrackingBroadcaster
import com.example.foodike.tracking.domain.port.TrackingRepository
import com.example.foodike.tracking.domain.service.TrackingEventConsumer
import com.example.foodike.tracking.domain.service.TrackingService
import com.example.foodike.tracking.infrastructure.persistence.ExposedTrackingRepository
import com.example.foodike.tracking.infrastructure.realtime.InMemoryTrackingBroadcaster
import org.koin.core.module.Module
import org.koin.dsl.module

val trackingModule: Module = module {
    single<TrackingRepository> { ExposedTrackingRepository() }
    single<TrackingBroadcaster> { InMemoryTrackingBroadcaster() }
    // OrderQueryPort is bound in the app composition root (it bridges to order-service).
    single { TrackingService(repository = get(), orderQuery = get(), broadcaster = get(), eventBus = get()) }
    single { TrackingEventConsumer(eventBus = get(), service = get()) }
}
