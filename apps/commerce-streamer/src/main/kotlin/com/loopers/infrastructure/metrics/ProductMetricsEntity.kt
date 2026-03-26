package com.loopers.infrastructure.metrics

import com.loopers.support.jpa.BaseEntity
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
class ProductMetricsEntity(
    id: Long?,

    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long,

    @Column(name = "view_count", nullable = false)
    val viewCount: Long = 0,

    @Column(name = "like_count", nullable = false)
    val likeCount: Long = 0,

    @Column(name = "order_count", nullable = false)
    val orderCount: Long = 0,

    @Column(name = "sales_amount", nullable = false)
    val salesAmount: Long = 0,
) : BaseEntity() {
    init {
        this.id = id
    }
}
