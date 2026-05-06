package com.example.foodike.user.infrastructure.persistence

import com.example.foodike.persistence.dbQuery
import com.example.foodike.user.domain.model.SsoIdentity
import com.example.foodike.user.domain.model.SsoProvider
import com.example.foodike.user.domain.port.SsoIdentityRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedSsoIdentityRepository : SsoIdentityRepository {
    override suspend fun findBySubject(provider: SsoProvider, subject: String): SsoIdentity? =
        dbQuery {
            SsoIdentitiesTable
                .selectAll()
                .where { (SsoIdentitiesTable.provider eq provider.name) and (SsoIdentitiesTable.subject eq subject) }
                .singleOrNull()
                ?.toSsoIdentity()
        }

    override suspend fun findByUserId(userId: String): List<SsoIdentity> =
        dbQuery {
            SsoIdentitiesTable
                .selectAll()
                .where { SsoIdentitiesTable.userId eq userId }
                .map { it.toSsoIdentity() }
        }

    override suspend fun save(identity: SsoIdentity): SsoIdentity =
        dbQuery {
            val existing = SsoIdentitiesTable
                .selectAll()
                .where { SsoIdentitiesTable.id eq identity.id }
                .singleOrNull()

            if (existing == null) {
                SsoIdentitiesTable.insert {
                    it[id] = identity.id
                    it[userId] = identity.userId
                    it[provider] = identity.provider.name
                    it[subject] = identity.subject
                    it[email] = identity.email
                    it[createdAt] = identity.createdAt
                    it[updatedAt] = identity.updatedAt
                }
            } else {
                SsoIdentitiesTable.update({ SsoIdentitiesTable.id eq identity.id }) {
                    it[email] = identity.email
                    it[updatedAt] = identity.updatedAt
                }
            }

            identity
        }

    override suspend fun deleteByUserAndProvider(userId: String, provider: SsoProvider) {
        dbQuery {
            SsoIdentitiesTable.deleteWhere {
                (SsoIdentitiesTable.userId eq userId) and (SsoIdentitiesTable.provider eq provider.name)
            }
        }
    }
}

private fun ResultRow.toSsoIdentity() =
    SsoIdentity(
        id = this[SsoIdentitiesTable.id],
        userId = this[SsoIdentitiesTable.userId],
        provider = SsoProvider.valueOf(this[SsoIdentitiesTable.provider]),
        subject = this[SsoIdentitiesTable.subject],
        email = this[SsoIdentitiesTable.email],
        createdAt = this[SsoIdentitiesTable.createdAt],
        updatedAt = this[SsoIdentitiesTable.updatedAt],
    )
