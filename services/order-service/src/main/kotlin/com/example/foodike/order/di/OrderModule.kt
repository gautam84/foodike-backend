package com.example.foodike.order.di

import com.example.foodike.order.domain.port.CartRepository
import com.example.foodike.order.domain.port.OrderRepository
import com.example.foodike.order.domain.service.CartService
import com.example.foodike.order.domain.service.OrderService
import com.example.foodike.order.infrastructure.persistence.ExposedCartRepository
import com.example.foodike.order.infrastructure.persistence.ExposedOrderRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val orderModule: Module = module {
    single<OrderRepository> { ExposedOrderRepository() }
    single<CartRepository> { ExposedCartRepository() }
    // CatalogQueryPort is bound in the app composition root (it bridges to restaurant-service).
    single { CartService(cartRepository = get(), catalog = get()) }
    single { OrderService(orderRepository = get(), cartRepository = get(), catalog = get(), eventBus = get()) }
}
