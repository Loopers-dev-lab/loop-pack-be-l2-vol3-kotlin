package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "product_metrics_daily",
    indexes = [
        Index(name = "idx_pmd_metric_date", columnList = "metric_date"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_pmd_product_date", columnNames = ["product_id", "metric_date"]),
    ],
)
class ProductMetricsDailyModel(
    productId: Long,
    metricDate: LocalDate,
) : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "metric_date", nullable = false)
    var metricDate: LocalDate = metricDate
        protected set

    @Column(name = "likes_count", nullable = false)
    var likesCount: Long = 0
        protected set

    @Column(name = "views_count", nullable = false)
    var viewsCount: Long = 0
        protected set

    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = 0
        protected set
}
