package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "mv_product_rank_weekly",
    indexes = [
        Index(name = "uk_weekly_period_rank", columnList = "period_date, ranking_rank", unique = true),
        Index(name = "idx_weekly_period_date", columnList = "period_date"),
    ],
)
class ProductRankWeekly(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "ranking_rank", nullable = false)
    val rankingRank: Int,

    @Column(name = "total_score", nullable = false)
    val totalScore: Double,

    @Column(name = "period_date", nullable = false, length = 8)
    val periodDate: String,
) : BaseEntity()
