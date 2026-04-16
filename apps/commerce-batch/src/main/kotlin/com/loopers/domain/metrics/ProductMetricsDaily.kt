package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * commerce-batch에서 product_metrics_daily 테이블 DDL 생성을 위한 최소 엔티티.
 */
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
    val metricDate: LocalDate = LocalDate.now(),

    @Column(name = "product_id", nullable = false)
    val productId: Long = 0,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @Column(nullable = false)
    var likeCount: Long = 0,

    @Column(nullable = false)
    var salesCount: Long = 0,
)
