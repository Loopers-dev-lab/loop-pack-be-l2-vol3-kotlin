package com.loopers.batch.job.ranking.domain

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "ranking_metric",
    indexes = [
        Index(name = "uk_ranking_metric_product_date", columnList = "product_id, ranking_date", unique = true),
        Index(name = "idx_ranking_metric_date", columnList = "ranking_date"),
        Index(name = "idx_ranking_metric_date_product_score", columnList = "ranking_date, product_id, total_score"),
    ],
)
class RankingMetric(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "ranking_date", nullable = false, length = 8)
    val rankingDate: String,

    @Column(name = "total_score", nullable = false)
    var totalScore: Double = 0.0,

    @Column(name = "event_count", nullable = false)
    var eventCount: Long = 0,
) : BaseEntity()
