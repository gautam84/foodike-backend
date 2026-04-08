package com.example.foodike.restaurant.domain.model

import com.example.foodike.common.model.Money
import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
)

@Serializable
data class MenuItem(
    val id: String,
    val name: String,
    val price: Money,
)
