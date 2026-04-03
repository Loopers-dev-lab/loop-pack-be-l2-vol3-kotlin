package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueEntryState
import com.loopers.application.queue.QueueStrategyType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "queue_entries",
    indexes = [
        Index(name = "idx_queue_entries_strategy_state_order", columnList = "strategy_type, state, queue_order"),
        Index(name = "idx_queue_entries_strategy_member", columnList = "strategy_type, member_id"),
    ],
)
class QueueEntryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    val strategyType: QueueStrategyType,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "queue_order", nullable = false)
    var queueOrder: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    var state: QueueEntryState,
    @Column(name = "token")
    var token: String? = null,
    @Column(name = "token_expires_at")
    var tokenExpiresAt: ZonedDateTime? = null,
    @Column(name = "source_offset")
    var sourceOffset: Long? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,
    @Column(name = "admitted_at")
    var admittedAt: ZonedDateTime? = null,
    @Column(name = "completed_at")
    var completedAt: ZonedDateTime? = null,
) {
    fun admit(token: String, tokenExpiresAt: ZonedDateTime) {
        this.state = QueueEntryState.ADMITTED
        this.token = token
        this.tokenExpiresAt = tokenExpiresAt
        this.admittedAt = ZonedDateTime.now()
    }

    fun complete() {
        this.state = QueueEntryState.COMPLETED
        this.completedAt = ZonedDateTime.now()
        this.token = null
        this.tokenExpiresAt = null
    }

    fun expire() {
        this.state = QueueEntryState.EXPIRED
        this.token = null
        this.tokenExpiresAt = null
    }

    fun isExpired(now: ZonedDateTime): Boolean {
        return state == QueueEntryState.ADMITTED && tokenExpiresAt?.isBefore(now) == true
    }

    @PrePersist
    fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
