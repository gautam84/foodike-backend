package com.example.foodike.user.di

import org.koin.core.module.Module
import org.koin.dsl.module

val userModule: Module = module {
    single { "user-service" }
}
