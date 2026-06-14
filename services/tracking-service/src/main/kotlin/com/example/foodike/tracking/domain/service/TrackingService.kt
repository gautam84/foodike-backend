package com.example.foodike.tracking.domain.service

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.Location
import com.example.foodike.events.EventBus
import com.example.foodike.events.TrackingEvents
import com.example.foodike.tracking.domain.model.DeliveryStatus
import com.example.foodike.tracking.domain.model.TrackingSession
import com.example.foodike.tracking.domain.model.TrackingUpdate
import com.example.foodike.tracking.domain.port.OrderQueryPort
import com.example.foodike.tracking.domain.port.TrackingBroadcaster
import com.example.foodike.tracking.domain.port.TrackingRepository
import java.time.Clock
import java.time.Instant

/** Permitted forward transitions; states absent as keys (DELIVERED, CANCELLED) are terminal. */
private val ALLOWED_TRANSITIONS: Map<DeliveryStatus, Set<DeliveryStatus>> = mapOf(
    DeliveryStatus.PENDING_ASSIGNMENT to setOf(DeliveryStatus.ASSIGNED, DeliveryStatus.CANCELLED),
    DeliveryStatus.ASSIGNED to setOf(DeliveryStatus.PICKED_UP, DeliveryStatus.CANCELLED),
    DeliveryStatus.PICKED_UP to setOf(DeliveryStatus.EN_ROUTE, DeliveryStatus.CANCELLED),
    DeliveryStatus.EN_ROUTE to setOf(DeliveryStatus.ARRIVED, DeliveryStatus.CANCELLED),
    DeliveryStatus.ARRIVED to setOf(DeliveryStatus.DELIVERED),
)

/** States during which a courier may push live location pings. */
private val ACTIVE_STATES = setOf(
    DeliveryStatus.ASSIGNED,
    DeliveryStatus.PICKED_UP,
    DeliveryStatus.EN_ROUTE,
    DeliveryStatus.ARRIVED,
)

class TrackingService(
    private val repository: TrackingRepository,
    private val orderQuery: OrderQueryPort,
    private val broadcaster: TrackingBroadcaster,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** Idempotently opens a tracking session for an order; called by the event consumer. */
    suspend fun createSession(orderId: String, customerId: String): TrackingSession {
        repository.findByOrderId(orderId)?.let { return it }
        val now = Instant.now(clock)
        return repository.create(
            TrackingSession(
                orderId = orderId,
                customerId = customerId,
                status = DeliveryStatus.PENDING_ASSIGNMENT,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun getSession(actor: Actor, orderId: String): TrackingSession {
        val order = orderQuery.findOrder(orderId) ?: throw NotFoundException("Order not found")
        actor.requireCanView(order.userId)
        return repository.findByOrderId(orderId) ?: createSession(orderId, order.userId)
    }

    suspend fun assignCourier(actor: Actor, orderId: String, courierId: String): TrackingSession {
        actor.requireStaffRole()
        val session = requireSession(orderId)
        val assigned = transition(session, DeliveryStatus.ASSIGNED).copy(courierId = courierId)
        val saved = repository.save(assigned)
        announce(saved)
        return saved
    }

    suspend fun recordLocation(actor: Actor, orderId: String, location: Location): TrackingSession {
        actor.requireCourierRole()
        val session = requireSession(orderId)
        if (session.status !in ACTIVE_STATES) {
            throw ConflictException("Delivery is not active for order $orderId")
        }
        val now = Instant.now(clock)
        val saved = repository.save(
            session.copy(latestLocation = location, locationUpdatedAt = now, updatedAt = now),
        )
        broadcaster.publish(orderId, TrackingUpdate(orderId, saved.status, location, now))
        return saved
    }

    suspend fun advanceStatus(actor: Actor, orderId: String, target: DeliveryStatus): TrackingSession {
        actor.requireStaffRole()
        val session = requireSession(orderId)
        val saved = repository.save(transition(session, target))
        announce(saved)
        return saved
    }

    /** Loads the session, opening one from the backing order if the event hasn't landed yet. */
    private suspend fun requireSession(orderId: String): TrackingSession {
        repository.findByOrderId(orderId)?.let { return it }
        val order = orderQuery.findOrder(orderId) ?: throw NotFoundException("Order not found")
        return createSession(orderId, order.userId)
    }

    private fun transition(session: TrackingSession, target: DeliveryStatus): TrackingSession {
        val allowed = ALLOWED_TRANSITIONS[session.status]
            ?: throw ConflictException("Delivery is in a terminal state: ${session.status}")
        if (target !in allowed) {
            throw ValidationException(
                "Invalid status transition",
                mapOf("status" to "Cannot move from ${session.status} to $target"),
            )
        }
        return session.copy(status = target, updatedAt = Instant.now(clock))
    }

    /** Fans the new status out to live subscribers and emits a domain event for other services. */
    private suspend fun announce(session: TrackingSession) {
        broadcaster.publish(
            session.orderId,
            TrackingUpdate(session.orderId, session.status, session.latestLocation, session.updatedAt),
        )
        eventBus.publish(
            TrackingEvents.deliveryStatusChanged(
                orderId = session.orderId,
                status = session.status.name,
                userId = session.customerId,
            ),
        )
    }
}
