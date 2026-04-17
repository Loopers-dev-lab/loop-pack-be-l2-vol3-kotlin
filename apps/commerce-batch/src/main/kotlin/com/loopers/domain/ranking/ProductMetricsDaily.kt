package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "product_metrics_daily",
    indexes = [Index(name = "idx_pmd_metric_date", columnList = "metric_date")],
)
class ProductMetricsDaily private constructor(
    id: ProductMetricsDailyId,
    viewCount: Long,
    likeCount: Long,
    orderCount: Long,
    totalScore: Double,
    rankPosition: Int?,
) {

    @EmbeddedId
    val id: ProductMetricsDailyId = id

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    @Column(name = "total_score", nullable = false)
    var totalScore: Double = totalScore
        protected set

    @Column(name = "rank_position")
    var rankPosition: Int? = rankPosition
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {
        updatedAt = ZonedDateTime.now()
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    val productId: Long get() = id.productId
    val metricDate: LocalDate get() = id.metricDate

    companion object {
        fun create(
            productId: Long,
            metricDate: LocalDate,
            viewCount: Long = 0L,
            likeCount: Long = 0L,
            orderCount: Long = 0L,
            totalScore: Double = 0.0,
            rankPosition: Int? = null,
        ): ProductMetricsDaily = ProductMetricsDaily(
            id = ProductMetricsDailyId(productId, metricDate),
            viewCount = viewCount,
            likeCount = likeCount,
            orderCount = orderCount,
            totalScore = totalScore,
            rankPosition = rankPosition,
        )
    }
}
