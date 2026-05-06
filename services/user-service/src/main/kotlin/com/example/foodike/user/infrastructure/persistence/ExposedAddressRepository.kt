package com.example.foodike.user.infrastructure.persistence

import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.persistence.dbQuery
import com.example.foodike.user.domain.model.Address
import com.example.foodike.user.domain.port.AddressRepository
import java.time.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedAddressRepository : AddressRepository {
    override suspend fun findByUserId(userId: String): List<Address> =
        dbQuery {
            AddressesTable
                .selectAll()
                .where { AddressesTable.userId eq userId }
                .orderBy(AddressesTable.isDefault to SortOrder.DESC, AddressesTable.createdAt to SortOrder.ASC)
                .map { it.toAddress() }
        }

    override suspend fun findByIdForUser(addressId: String, userId: String): Address? =
        dbQuery {
            AddressesTable
                .selectAll()
                .where { (AddressesTable.id eq addressId) and (AddressesTable.userId eq userId) }
                .singleOrNull()
                ?.toAddress()
        }

    override suspend fun create(address: Address): Address =
        dbQuery {
            if (address.isDefault) {
                AddressesTable.update({ AddressesTable.userId eq address.userId }) {
                    it[isDefault] = false
                    it[updatedAt] = Instant.now()
                }
            }
            AddressesTable.insert {
                it[id] = address.id
                it[userId] = address.userId
                it[label] = address.label
                it[line1] = address.line1
                it[line2] = address.line2
                it[city] = address.city
                it[state] = address.state
                it[postalCode] = address.postalCode
                it[country] = address.country
                it[latitude] = address.latitude
                it[longitude] = address.longitude
                it[isDefault] = address.isDefault
                it[createdAt] = address.createdAt
                it[updatedAt] = address.updatedAt
            }
            address
        }

    override suspend fun update(address: Address): Address =
        dbQuery {
            val rows = AddressesTable.update({
                (AddressesTable.id eq address.id) and (AddressesTable.userId eq address.userId)
            }) {
                it[label] = address.label
                it[line1] = address.line1
                it[line2] = address.line2
                it[city] = address.city
                it[state] = address.state
                it[postalCode] = address.postalCode
                it[country] = address.country
                it[latitude] = address.latitude
                it[longitude] = address.longitude
                it[updatedAt] = address.updatedAt
            }
            if (rows == 0) throw NotFoundException("Address not found")
            address
        }

    override suspend fun delete(addressId: String, userId: String): Boolean =
        dbQuery {
            val rows = AddressesTable.deleteWhere {
                (AddressesTable.id eq addressId) and (AddressesTable.userId eq userId)
            }
            rows > 0
        }

    override suspend fun setDefault(addressId: String, userId: String): Address =
        dbQuery {
            val target = AddressesTable
                .selectAll()
                .where { (AddressesTable.id eq addressId) and (AddressesTable.userId eq userId) }
                .singleOrNull()
                ?.toAddress()
                ?: throw NotFoundException("Address not found")

            val now = Instant.now()
            AddressesTable.update({
                (AddressesTable.userId eq userId) and (AddressesTable.id neq addressId)
            }) {
                it[isDefault] = false
                it[updatedAt] = now
            }
            AddressesTable.update({
                (AddressesTable.id eq addressId) and (AddressesTable.userId eq userId)
            }) {
                it[isDefault] = true
                it[updatedAt] = now
            }
            target.copy(isDefault = true, updatedAt = now)
        }

    override suspend fun countForUser(userId: String): Long =
        dbQuery {
            AddressesTable
                .selectAll()
                .where { AddressesTable.userId eq userId }
                .count()
        }
}

private fun ResultRow.toAddress() =
    Address(
        id = this[AddressesTable.id],
        userId = this[AddressesTable.userId],
        label = this[AddressesTable.label],
        line1 = this[AddressesTable.line1],
        line2 = this[AddressesTable.line2],
        city = this[AddressesTable.city],
        state = this[AddressesTable.state],
        postalCode = this[AddressesTable.postalCode],
        country = this[AddressesTable.country],
        latitude = this[AddressesTable.latitude],
        longitude = this[AddressesTable.longitude],
        isDefault = this[AddressesTable.isDefault],
        createdAt = this[AddressesTable.createdAt],
        updatedAt = this[AddressesTable.updatedAt],
    )
