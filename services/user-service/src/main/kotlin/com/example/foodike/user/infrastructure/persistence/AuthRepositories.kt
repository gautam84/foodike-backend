package com.example.foodike.user.infrastructure.persistence

import com.example.foodike.auth.UserRole
import com.example.foodike.persistence.dbQuery
import com.example.foodike.user.domain.model.RefreshTokenRecord
import com.example.foodike.user.domain.model.User
import com.example.foodike.user.domain.port.RefreshTokenStore
import com.example.foodike.user.domain.port.UserRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedUserRepository : UserRepository {
    override suspend fun findById(id: String): User? =
        dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun findByPhone(phone: String): User? =
        dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.phone eq phone }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun findByGoogleId(googleId: String): User? =
        dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.googleId eq googleId }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun findByEmail(email: String): User? =
        dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun save(user: User): User =
        dbQuery {
            val existing = UsersTable
                .selectAll()
                .where { UsersTable.id eq user.id }
                .singleOrNull()

            if (existing == null) {
                UsersTable.insert {
                    it[id] = user.id
                    it[phone] = user.phone
                    it[googleId] = user.googleId
                    it[email] = user.email
                    it[name] = user.name
                    it[avatarUrl] = user.avatarUrl
                    it[role] = user.role.name
                    val now = java.time.Instant.now()
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            } else {
                UsersTable.update({ UsersTable.id eq user.id }) {
                    it[phone] = user.phone
                    it[googleId] = user.googleId
                    it[email] = user.email
                    it[name] = user.name
                    it[avatarUrl] = user.avatarUrl
                    it[role] = user.role.name
                    it[updatedAt] = java.time.Instant.now()
                }
            }

            user
        }
}

class ExposedRefreshTokenStore : RefreshTokenStore {
    override suspend fun save(record: RefreshTokenRecord) {
        dbQuery {
            RefreshTokensTable.deleteWhere { userId eq record.userId }
            RefreshTokensTable.insert {
                it[id] = record.id
                it[userId] = record.userId
                it[tokenHash] = record.tokenHash
                it[expiresAt] = record.expiresAt
                it[createdAt] = record.createdAt
            }
        }
    }

    override suspend fun findValidByUserId(userId: String): RefreshTokenRecord? =
        dbQuery {
            RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.userId eq userId }
                .singleOrNull()
                ?.toRefreshTokenRecord()
                ?.takeIf { it.expiresAt.isAfter(java.time.Instant.now()) }
        }

    override suspend fun deleteByUserId(userId: String) {
        dbQuery {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }
    }
}

private fun ResultRow.toUser() =
    User(
        id = this[UsersTable.id],
        phone = this[UsersTable.phone],
        googleId = this[UsersTable.googleId],
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        avatarUrl = this[UsersTable.avatarUrl],
        role = UserRole.valueOf(this[UsersTable.role]),
    )

private fun ResultRow.toRefreshTokenRecord() =
    RefreshTokenRecord(
        id = this[RefreshTokensTable.id],
        userId = this[RefreshTokensTable.userId],
        tokenHash = this[RefreshTokensTable.tokenHash],
        expiresAt = this[RefreshTokensTable.expiresAt],
        createdAt = this[RefreshTokensTable.createdAt],
    )
