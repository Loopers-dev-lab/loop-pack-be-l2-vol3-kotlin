package com.loopers.domain.deadletter

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "failed_events",
    indexes = [
        Index(name = "idx_failed_events_status", columnList = "status, created_at"),
    ],
)
class FailedEvent(
    @Column(name = "topic", nullable = false)
    val topic: String,

    @Column(name = "event_key")
    val eventKey: String?,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String?,

    @Column(name = "event_id")
    val eventId: Long? = null,

    @Column(name = "event_type")
    val eventType: String? = null,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: FailedEventStatus = FailedEventStatus.PENDING
        protected set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        protected set

    fun markRetried() {
        retryCount++
    }

    fun markResolved() {
        status = FailedEventStatus.RESOLVED
    }
}

enum class FailedEventStatus {
    PENDING,
    RESOLVED,
}
