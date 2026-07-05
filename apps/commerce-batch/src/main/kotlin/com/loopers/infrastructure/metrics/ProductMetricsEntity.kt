package com.loopers.infrastructure.metrics

import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_product_metrics_product_id_bucket_date", columnNames = ["product_id", "bucket_date"]),
    ],
    indexes = [
        Index(name = "idx_product_metrics_bucket_date", columnList = "bucket_date"),
    ],
)
class ProductMetricsEntity(
    id: Long?,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "bucket_date", nullable = false)
    val bucketDate: LocalDate,

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
