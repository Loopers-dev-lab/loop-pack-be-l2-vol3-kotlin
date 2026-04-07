package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductMetricsJpaRepository : JpaRepository<ProductMetrics, Long> {
    fun findByProductId(productId: Long): ProductMetrics?

    @Modifying
    @Query(
        "UPDATE ProductMetrics m SET m.likeCount = m.likeCount + 1, m.version = :eventVersion " +
            "WHERE m.productId = :productId AND m.version < :eventVersion",
    )
    fun incrementLikeCount(productId: Long, eventVersion: Long): Int

    @Modifying
    @Query(
        "UPDATE ProductMetrics m SET m.likeCount = CASE WHEN m.likeCount > 0 THEN m.likeCount - 1 ELSE 0 END, " +
            "m.version = :eventVersion WHERE m.productId = :productId AND m.version < :eventVersion",
    )
    fun decrementLikeCount(productId: Long, eventVersion: Long): Int

    @Modifying
    @Query(
        "UPDATE ProductMetrics m SET m.viewCount = m.viewCount + 1, m.version = :eventVersion " +
            "WHERE m.productId = :productId AND m.version < :eventVersion",
    )
    fun incrementViewCount(productId: Long, eventVersion: Long): Int

    @Modifying
    @Query(
        "UPDATE ProductMetrics m SET m.orderCount = m.orderCount + 1, m.version = :eventVersion " +
            "WHERE m.productId = :productId AND m.version < :eventVersion",
    )
    fun incrementOrderCount(productId: Long, eventVersion: Long): Int
}
