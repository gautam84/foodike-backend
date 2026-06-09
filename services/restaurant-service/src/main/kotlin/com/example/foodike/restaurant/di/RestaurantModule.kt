package com.example.foodike.restaurant.di

import com.example.foodike.restaurant.domain.port.MenuRepository
import com.example.foodike.restaurant.domain.port.RestaurantRepository
import com.example.foodike.restaurant.domain.port.ReviewRepository
import com.example.foodike.restaurant.domain.service.MenuService
import com.example.foodike.restaurant.domain.service.RestaurantService
import com.example.foodike.restaurant.domain.service.ReviewService
import com.example.foodike.restaurant.infrastructure.persistence.ExposedMenuRepository
import com.example.foodike.restaurant.infrastructure.persistence.ExposedRestaurantRepository
import com.example.foodike.restaurant.infrastructure.persistence.ExposedReviewRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val restaurantModule: Module = module {
    single<RestaurantRepository> { ExposedRestaurantRepository() }
    single<MenuRepository> { ExposedMenuRepository() }
    single<ReviewRepository> { ExposedReviewRepository() }
    single { RestaurantService(restaurantRepository = get()) }
    single { MenuService(menuRepository = get(), restaurantRepository = get()) }
    single { ReviewService(reviewRepository = get(), restaurantRepository = get()) }
}
