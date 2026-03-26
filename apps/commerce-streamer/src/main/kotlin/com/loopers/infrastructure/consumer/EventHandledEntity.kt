package com.loopers.infrastructure.consumer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "event_handled",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_handled_consumer_event", columnNames = ["consumer_group", "event_id"]),
    ],
    indexes = [
        Index(name = "idx_event_handled_event_id", columnList = "event_id"),
    ],
)
class EventHandledEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "consumer_group", nullable = false, length = 120)
    val consumerGroup: String,

    @Column(name = "event_id", nullable = false, length = 120)
    val eventId: String,

    @Column(name = "handled_at", nullable = false)
    var handledAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        handledAt = ZonedDateTime.now()
    }
}
