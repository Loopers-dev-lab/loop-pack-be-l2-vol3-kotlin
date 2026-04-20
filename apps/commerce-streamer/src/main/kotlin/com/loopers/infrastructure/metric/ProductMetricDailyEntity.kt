package com.loopers.infrastructure.metric

import com.loopers.domain.metric.ProductMetricDaily
import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Table(
    name = "product_metrics_daily",
    indexes = [
        Index(
            name = "idx_product_metrics_daily_product_id_metric_date",
            columnList = "product_id, metric_date",
            unique = true,
        ),
    ],
)
@Entity
class ProductMetricDailyEntity(
    id: Long? = null,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,
    @Column(name = "view_count", nullable = false)
    var viewCount: Int = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,
    @Column(name = "units_sold", nullable = false)
    var unitsSold: Int = 0,
    @Column(name = "sales_amount", nullable = false)
    var salesAmount: Long = 0L,
    @Column(name = "order_score", nullable = false)
    var orderScore: Double = 0.0,
) : BaseEntity() {
    init {
        this.id = id
    }

    fun toDomain(): ProductMetricDaily = ProductMetricDaily.retrieve(
        productId = productId,
        metricDate = metricDate,
        viewCount = viewCount,
        likeCount = likeCount,
        unitsSold = unitsSold,
        salesAmount = salesAmount,
        orderScore = orderScore,
    )

    fun apply(metric: ProductMetricDaily) {
        viewCount = metric.viewCount
        likeCount = metric.likeCount
        unitsSold = metric.unitsSold
        salesAmount = metric.salesAmount
        orderScore = metric.orderScore
    }
}
