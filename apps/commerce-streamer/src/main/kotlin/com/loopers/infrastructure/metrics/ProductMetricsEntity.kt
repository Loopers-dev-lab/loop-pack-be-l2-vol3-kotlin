package com.loopers.infrastructure.metrics

import com.loopers.domain.BaseEntity
import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "product_metrics",
    indexes = [Index(name = "idx_product_metrics_product_id", columnList = "product_id", unique = true)],
)
class ProductMetricsEntity(
    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long,
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = 0,
) : BaseEntity() {

    companion object {
        fun fromDomain(metrics: ProductMetrics): ProductMetricsEntity {
            return ProductMetricsEntity(
                productId = metrics.productId,
                viewCount = metrics.viewCount,
                likeCount = metrics.likeCount,
                salesCount = metrics.salesCount,
            ).withBaseFields(id = metrics.id)
        }
    }

    fun toDomain(): ProductMetrics = ProductMetrics(
        id = id,
        productId = productId,
        viewCount = viewCount,
        likeCount = likeCount,
        salesCount = salesCount,
    )
}
