package com.loopers.infrastructure.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_product_metrics_metric_date_product_id",
            columnNames = ["metric_date", "product_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_product_metrics_metric_date_product_id", columnList = "metric_date, product_id"),
        Index(name = "idx_product_metrics_product_id", columnList = "product_id"),
    ],
)
class ProductMetricsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L,

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0L,

    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = 0L,

    @Column(name = "last_like_event_at")
    var lastLikeEventAt: ZonedDateTime? = null,

    @Column(name = "last_view_event_at")
    var lastViewEventAt: ZonedDateTime? = null,

    @Column(name = "last_sales_event_at")
    var lastSalesEventAt: ZonedDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
