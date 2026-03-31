package com.loopers.domain.event

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "event_handled")
class EventHandledModel(
    eventId: String,
    eventType: String,
) : BaseEntity() {
    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    var eventId: String = eventId
        protected set

    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: String = eventType
        protected set
}
