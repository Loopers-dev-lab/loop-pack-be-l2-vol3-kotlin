package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "ranking_event_log",
    indexes = [
        Index(name = "idx_ranking_event_log_date_product", columnList = "occurred_date, product_id"),
    ],
)
class RankingEventLog(
    productId: Long,
    eventType: String,
    eventValue: Double,
    occurredDate: LocalDate,
    eventId: String,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId

    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: String = eventType

    @Column(name = "event_value", nullable = false)
    val eventValue: Double = eventValue

    @Column(name = "occurred_date", nullable = false)
    val occurredDate: LocalDate = occurredDate

    @Column(name = "event_id", nullable = false)
    val eventId: String = eventId

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
