package com.loopers.infrastructure.metric

import com.loopers.domain.metric.HandledEvent
import com.loopers.infrastructure.BaseEntity
import com.loopers.infrastructure.outbox.KafkaEventType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Table(
    name = "event_handled",
    indexes = [
        Index(name = "idx_event_handled_event_id", columnList = "event_id", unique = true),
    ],
)
@Entity
class HandledEventEntity(
    id: Long? = null,
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: Long,
    @Column(name = "topic", nullable = false)
    val topic: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: KafkaEventType,
) : BaseEntity() {
    init {
        this.id = id
    }

    fun toDomain(): HandledEvent = HandledEvent(
        eventId = eventId,
        topic = topic,
        eventType = eventType,
    )
}
