package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "product_metrics",
    indexes = [
        Index(name = "idx_product_metrics_product_id", columnList = "product_id", unique = true),
    ],
)
class ProductMetrics(
    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long,
) : BaseEntity() {
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0
        protected set

    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = 0
        protected set

    @Column(name = "revenue", nullable = false)
    var revenue: Long = 0
        protected set

    fun incrementLikeCount() {
        likeCount++
    }

    fun decrementLikeCount() {
        if (likeCount > 0) likeCount--
    }

    fun incrementOrderCount() {
        orderCount++
    }

    fun addOrderCount(count: Long) {
        orderCount += count
    }

    fun addSales(count: Long, amount: Long) {
        salesCount += count
        revenue += amount
    }
}
