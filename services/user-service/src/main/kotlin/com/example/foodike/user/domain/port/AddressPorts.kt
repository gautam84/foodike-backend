package com.example.foodike.user.domain.port

import com.example.foodike.user.domain.model.Address

interface AddressRepository {
    suspend fun findByUserId(userId: String): List<Address>

    suspend fun findByIdForUser(addressId: String, userId: String): Address?

    suspend fun create(address: Address): Address

    suspend fun update(address: Address): Address

    suspend fun delete(addressId: String, userId: String): Boolean

    suspend fun setDefault(addressId: String, userId: String): Address

    suspend fun countForUser(userId: String): Long
}
