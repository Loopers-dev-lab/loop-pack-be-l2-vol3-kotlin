package com.loopers.domain.catalog

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "product_metrics",
    indexes = [
        Index(name = "uk_product_metrics_product_id", columnList = "product_id", unique = true),
        Index(name = "idx_product_metrics_order_count", columnList = "order_count DESC"),
        Index(name = "idx_product_metrics_like_count", columnList = "like_count DESC"),
        Index(name = "idx_product_metrics_view_count", columnList = "view_count DESC"),
    ],
)
class ProductMetricsModel(
    productId: Long,
    viewCount: Long = 0,
    likeCount: Long = 0,
    orderCount: Long = 0,
) : BaseEntity() {

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    fun updateMetrics(viewCount: Long, likeCount: Long, orderCount: Long) {
        this.viewCount = viewCount
        this.likeCount = likeCount
        this.orderCount = orderCount
    }
}
