package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "tb_product_metrics",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["product_id", "metrics_date"]),
    ],
)
class ProductMetricsModel(
    productId: Long,
    metricsDate: LocalDate,
    viewCount: Long = 0L,
    likeCount: Long = 0L,
    salesCount: Long = 0L,
    rankingScore: Double = 0.0,
    lastEventAt: ZonedDateTime? = null,
) : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "metrics_date", nullable = false)
    var metricsDate: LocalDate = metricsDate
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = salesCount
        protected set

    @Column(name = "ranking_score", nullable = false)
    var rankingScore: Double = rankingScore
        protected set

    @Column(name = "last_event_at")
    var lastEventAt: ZonedDateTime? = lastEventAt
        protected set
}
