package com.example.foodike.di

import com.example.foodike.adapters.CatalogQueryAdapter
import com.example.foodike.adapters.OrderQueryAdapter
import com.example.foodike.events.EventBus
import com.example.foodike.events.InProcessEventBus
import com.example.foodike.notification.di.notificationModule
import com.example.foodike.order.domain.port.CatalogQueryPort
import com.example.foodike.order.di.orderModule
import com.example.foodike.tracking.domain.port.OrderQueryPort
import com.example.foodike.payment.di.paymentModule
import com.example.foodike.restaurant.di.restaurantModule
import com.example.foodike.tracking.di.trackingModule
import com.example.foodike.user.di.userModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {
    single<EventBus> { InProcessEventBus() }
    single<CatalogQueryPort> {
        CatalogQueryAdapter(menuRepository = get(), restaurantRepository = get())
    }
    single<OrderQueryPort> { OrderQueryAdapter(orderRepository = get()) }
}

val serviceModules = listOf(
    appModule,
    userModule,
    restaurantModule,
    orderModule,
    paymentModule,
    notificationModule,
    trackingModule,
)
