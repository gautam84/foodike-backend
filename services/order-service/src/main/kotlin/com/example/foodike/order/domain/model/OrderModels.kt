package com.example.foodike.order.domain.model

import com.example.foodike.common.model.Money
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    CREATED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
}

@Serializable
data class CartItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int,
)

@Serializable
data class Cart(
    val id: String,
    val userId: String,
    val restaurantId: String? = null,
    val items: List<CartItem> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

@Serializable
data class OrderItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int,
    val lineTotal: Money,
)

@Serializable
data class Order(
    val id: String,
    val userId: String,
    val restaurantId: String,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Money,
    val deliveryFee: Money,
    val total: Money,
    val status: OrderStatus = OrderStatus.CREATED,
    val addressId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val placedAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

internal object InstantSerializer : kotlinx.serialization.KSerializer<Instant> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            "java.time.Instant",
            kotlinx.serialization.descriptors.PrimitiveKind.STRING,
        )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Instant =
        Instant.parse(decoder.decodeString())
}
