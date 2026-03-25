package com.loopers.infrastructure.event

import com.loopers.domain.event.model.EventHandled
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "event_handled")
class EventHandledEntity(
    @Id
    @Column(name = "event_id", nullable = false)
    val eventId: String,
    @Column(name = "handled_at", nullable = false)
    val handledAt: ZonedDateTime,
) {

    companion object {
        fun fromDomain(eventHandled: EventHandled): EventHandledEntity {
            return EventHandledEntity(
                eventId = eventHandled.eventId,
                handledAt = eventHandled.handledAt,
            )
        }
    }

    fun toDomain(): EventHandled = EventHandled(
        eventId = eventId,
        handledAt = handledAt,
    )
}
