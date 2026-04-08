package com.loopers.infrastructure.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "product_metrics")
class ProductMetricsEntity(
    @Id
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
