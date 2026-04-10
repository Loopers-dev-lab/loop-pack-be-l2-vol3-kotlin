package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "ranking_event",
    indexes = [
        Index(name = "idx_ranking_event_agg_created", columnList = "aggregated, created_at"),
    ],
)
class RankingEvent(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: RankingEventType,

    @Column(name = "score", nullable = false)
    val score: Double,

    @Column(name = "raw_count", nullable = false)
    val rawCount: Long = 1,

    @Column(name = "event_id", nullable = false)
    val eventId: String,

    @Column(name = "aggregated", nullable = false)
    var aggregated: Boolean = false,
) : BaseEntity()
