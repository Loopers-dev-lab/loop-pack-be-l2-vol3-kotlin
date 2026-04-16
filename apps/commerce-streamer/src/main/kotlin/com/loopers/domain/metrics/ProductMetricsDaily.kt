package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "product_metrics_daily",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["metric_date", "product_id"]),
    ],
)
class ProductMetricsDaily(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @Column(nullable = false)
    var likeCount: Long = 0,

    @Column(nullable = false)
    var salesCount: Long = 0,
)
