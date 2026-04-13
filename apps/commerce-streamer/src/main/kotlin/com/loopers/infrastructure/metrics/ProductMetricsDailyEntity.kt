package com.loopers.infrastructure.metrics

import com.loopers.domain.BaseEntity
import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "product_metrics_daily",
    uniqueConstraints = [UniqueConstraint(columnNames = ["metric_date", "product_id"])],
)
class ProductMetricsDailyEntity(
    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = 0,
) : BaseEntity() {

    companion object {
        fun fromDomain(daily: ProductMetricsDaily): ProductMetricsDailyEntity {
            return ProductMetricsDailyEntity(
                metricDate = daily.metricDate,
                productId = daily.productId,
                viewCount = daily.viewCount,
                likeCount = daily.likeCount,
                salesCount = daily.salesCount,
            ).withBaseFields(id = daily.id)
        }
    }

    fun toDomain(): ProductMetricsDaily = ProductMetricsDaily(
        id = id,
        productId = productId,
        metricDate = metricDate,
        viewCount = viewCount,
        likeCount = likeCount,
        salesCount = salesCount,
    )
}
