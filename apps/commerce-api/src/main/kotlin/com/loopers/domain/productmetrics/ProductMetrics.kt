package com.loopers.domain.productmetrics

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "product_metrics")
class ProductMetrics(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val productId: Long,

    var viewCount: Long = 0,

    var likeCount: Long = 0,

    val updatedAt: ZonedDateTime? = null,
) {
    fun incrementViewCount() {
        viewCount++
    }

    fun setLikeCount(count: Long) {
        likeCount = count
    }

    companion object {
        fun create(productId: Long): ProductMetrics {
            return ProductMetrics(
                productId = productId,
                viewCount = 0,
                likeCount = 0,
            )
        }
    }
}
