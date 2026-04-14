package com.loopers.batch.ranking.entity

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "product_metrics_daily",
    uniqueConstraints = [
        UniqueConstraint(
        name = "uq_product_metrics_daily_date_product",
        columnNames = ["metric_date", "product_id"],
    ),
    ],
)
class ProductMetricsDailyBatchEntity(
    @Column(name = "metric_date", nullable = false) val metricDate: LocalDate,
    @Column(name = "product_id", nullable = false) val productId: Long,
    @Column(name = "view_count", nullable = false) var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false) var likeCount: Long = 0,
    @Column(name = "sales_count", nullable = false) var salesCount: Long = 0,
) : BaseEntity()
