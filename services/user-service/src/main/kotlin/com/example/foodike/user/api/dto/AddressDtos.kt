package com.example.foodike.user.api.dto

import com.example.foodike.user.domain.model.Address
import kotlinx.serialization.Serializable

@Serializable
data class CreateAddressRequest(
    val label: String,
    val line1: String,
    val line2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean? = null,
)

@Serializable
data class UpdateAddressRequest(
    val label: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class AddressResponse(
    val id: String,
    val label: String,
    val line1: String,
    val line2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(address: Address): AddressResponse =
            AddressResponse(
                id = address.id,
                label = address.label,
                line1 = address.line1,
                line2 = address.line2,
                city = address.city,
                state = address.state,
                postalCode = address.postalCode,
                country = address.country,
                latitude = address.latitude,
                longitude = address.longitude,
                isDefault = address.isDefault,
                createdAt = address.createdAt.toString(),
                updatedAt = address.updatedAt.toString(),
            )
    }
}

@Serializable
data class AddressListResponse(
    val addresses: List<AddressResponse>,
)
