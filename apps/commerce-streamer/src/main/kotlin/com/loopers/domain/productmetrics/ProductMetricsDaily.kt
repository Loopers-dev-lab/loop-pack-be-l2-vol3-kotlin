package com.loopers.domain.productmetrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "product_metrics_daily")
class ProductMetricsDaily(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "product_id")
    val productId: Long,

    @Column(name = "view_count")
    val viewCount: Long = 0,

    @Column(name = "sales_count")
    val salesCount: Long = 0,

    @Column(name = "like_count")
    val likeCount: Long = 0,

    @Column(name = "metric_date")
    val metricDate: LocalDate,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
