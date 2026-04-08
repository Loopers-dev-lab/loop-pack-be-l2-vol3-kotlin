package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "event_handled")
class EventHandledRecord(
    @Id
    @Column(name = "event_id")
    val eventId: Long,

    @Column(name = "handled_at", nullable = false)
    val handledAt: ZonedDateTime = ZonedDateTime.now(),
)
