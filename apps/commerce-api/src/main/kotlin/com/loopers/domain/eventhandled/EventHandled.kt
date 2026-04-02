package com.loopers.domain.eventhandled

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "event_handled",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["dedupeKey"], name = "uk_event_handled_dedupe_key"),
    ],
)
class EventHandled(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val dedupeKey: String,

    val createdAt: ZonedDateTime? = null,
)
