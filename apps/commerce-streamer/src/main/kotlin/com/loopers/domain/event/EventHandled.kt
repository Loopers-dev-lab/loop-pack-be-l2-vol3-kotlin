package com.loopers.domain.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "event_handled")
class EventHandled(
    eventId: String,
    aggregateType: String,
    aggregateId: String,
    eventType: String,
) {

    @Id
    @Column(name = "event_id")
    val eventId: String = eventId

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String = aggregateType

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String = aggregateId

    @Column(name = "event_type", nullable = false)
    val eventType: String = eventType

    @Column(name = "handled_at", nullable = false, updatable = false)
    lateinit var handledAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        handledAt = ZonedDateTime.now()
    }
}
