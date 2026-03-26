package com.loopers.domain.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "event_handled")
class EventHandled private constructor(
    eventId: String,
    eventType: String,
) {

    @Id
    @Column(name = "event_id", length = 36)
    val eventId: String = eventId

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String = eventType

    @Column(name = "handled_at", nullable = false)
    lateinit var handledAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {
        handledAt = ZonedDateTime.now()
    }

    companion object {
        fun create(eventId: String, eventType: String): EventHandled {
            return EventHandled(eventId = eventId, eventType = eventType)
        }
    }
}
