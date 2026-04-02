package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "event_handled")
class EventHandledModel(
    eventId: String,
) {
    @Id
    @Column(name = "event_id", nullable = false, length = 100)
    var eventId: String = eventId
        protected set

    @Column(name = "handled_at", nullable = false)
    var handledAt: ZonedDateTime = ZonedDateTime.now()
        protected set
}
