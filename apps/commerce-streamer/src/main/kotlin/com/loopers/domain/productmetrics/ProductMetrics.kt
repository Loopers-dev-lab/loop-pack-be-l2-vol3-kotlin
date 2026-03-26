package com.loopers.domain.productmetrics

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "product_metrics")
class ProductMetrics(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val productId: Long,
    val viewCount: Long = 0,
    val salesCount: Long = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(productId: Long) = ProductMetrics(
            productId = productId,
            viewCount = 0,
            salesCount = 0,
        )
    }
}
