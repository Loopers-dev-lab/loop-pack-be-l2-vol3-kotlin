package com.loopers.batch.ranking.entity

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "product_metrics_daily")
class ProductMetricsDailyBatchEntity(
    @Column(name = "metric_date") val metricDate: LocalDate,
    @Column(name = "product_id") val productId: Long,
    @Column(name = "view_count") var viewCount: Long = 0,
    @Column(name = "like_count") var likeCount: Long = 0,
    @Column(name = "sales_count") var salesCount: Long = 0,
) : BaseEntity()
