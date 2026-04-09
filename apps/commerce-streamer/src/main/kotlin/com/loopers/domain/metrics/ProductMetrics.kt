package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.ZonedDateTime

@Entity
@Table(name = "product_metrics")
class ProductMetrics private constructor(
    productId: Long,
) {

    @Id
    @Column(name = "product_id")
    val productId: Long = productId

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0
        protected set

    @Column(name = "total_revenue", nullable = false)
    var totalRevenue: Long = 0
        protected set

    @Column(name = "last_event_at")
    var lastEventAt: ZonedDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    fun incrementViewCount() {
        viewCount++
        lastEventAt = ZonedDateTime.now()
    }

    fun incrementLikeCount() {
        likeCount++
        lastEventAt = ZonedDateTime.now()
    }

    fun decrementLikeCount() {
        if (likeCount > 0) likeCount--
        lastEventAt = ZonedDateTime.now()
    }

    fun incrementOrderCount(productCount: Int, revenue: Long) {
        orderCount += productCount
        totalRevenue += revenue
        lastEventAt = ZonedDateTime.now()
    }

    companion object {
        fun create(productId: Long): ProductMetrics {
            return ProductMetrics(productId = productId)
        }
    }
}
