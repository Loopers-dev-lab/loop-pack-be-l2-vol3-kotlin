package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "product_metrics")
class ProductMetrics(
    productId: Long,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long = productId

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        private set

    @Column(name = "order_count", nullable = false)
    var orderCount: Int = 0
        private set

    @Column(name = "view_count", nullable = false)
    var viewCount: Int = 0
        private set

    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        private set

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

    fun incrementLikeCount(eventVersion: Long) {
        likeCount++
        updateVersion(eventVersion)
    }

    fun decrementLikeCount(eventVersion: Long) {
        if (likeCount > 0) likeCount--
        updateVersion(eventVersion)
    }

    fun incrementViewCount(eventVersion: Long) {
        viewCount++
        updateVersion(eventVersion)
    }

    fun incrementOrderCount(eventVersion: Long) {
        orderCount++
        updateVersion(eventVersion)
    }

    fun isStaleEvent(eventVersion: Long): Boolean = eventVersion <= version

    private fun updateVersion(eventVersion: Long) {
        version = eventVersion
    }
}
