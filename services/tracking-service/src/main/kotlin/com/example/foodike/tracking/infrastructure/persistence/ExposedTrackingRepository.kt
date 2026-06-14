package com.example.foodike.tracking.infrastructure.persistence

import com.example.foodike.common.model.Location
import com.example.foodike.persistence.dbQuery
import com.example.foodike.tracking.domain.model.DeliveryStatus
import com.example.foodike.tracking.domain.model.TrackingSession
import com.example.foodike.tracking.domain.port.TrackingRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ExposedTrackingRepository : TrackingRepository {
    override suspend fun findByOrderId(orderId: String): TrackingSession? =
        dbQuery {
            TrackingSessionsTable
                .selectAll()
                .where { TrackingSessionsTable.orderId eq orderId }
                .singleOrNull()
                ?.toSession()
        }

    override suspend fun create(session: TrackingSession): TrackingSession =
        dbQuery {
            TrackingSessionsTable.insert { it.fromSession(session) }
            session
        }

    override suspend fun save(session: TrackingSession): TrackingSession =
        dbQuery {
            TrackingSessionsTable.update({ TrackingSessionsTable.orderId eq session.orderId }) {
                it.fromSession(session)
            }
            session
        }
}

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromSession(session: TrackingSession) {
    this[TrackingSessionsTable.orderId] = session.orderId
    this[TrackingSessionsTable.customerId] = session.customerId
    this[TrackingSessionsTable.courierId] = session.courierId
    this[TrackingSessionsTable.status] = session.status.name
    this[TrackingSessionsTable.lat] = session.latestLocation?.lat
    this[TrackingSessionsTable.lng] = session.latestLocation?.lng
    this[TrackingSessionsTable.locationUpdatedAt] = session.locationUpdatedAt
    this[TrackingSessionsTable.createdAt] = session.createdAt
    this[TrackingSessionsTable.updatedAt] = session.updatedAt
}

private fun ResultRow.toSession(): TrackingSession {
    val lat = this[TrackingSessionsTable.lat]
    val lng = this[TrackingSessionsTable.lng]
    return TrackingSession(
        orderId = this[TrackingSessionsTable.orderId],
        customerId = this[TrackingSessionsTable.customerId],
        courierId = this[TrackingSessionsTable.courierId],
        status = DeliveryStatus.valueOf(this[TrackingSessionsTable.status]),
        latestLocation = if (lat != null && lng != null) Location(lat = lat, lng = lng) else null,
        locationUpdatedAt = this[TrackingSessionsTable.locationUpdatedAt],
        createdAt = this[TrackingSessionsTable.createdAt],
        updatedAt = this[TrackingSessionsTable.updatedAt],
    )
}
