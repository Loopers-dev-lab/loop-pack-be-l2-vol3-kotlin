package com.loopers.infrastructure.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "event_handled")
class EventHandledEntity(
    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    val eventId: String,

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Column(name = "handled_at", nullable = false)
    val handledAt: ZonedDateTime = ZonedDateTime.now(),
)
