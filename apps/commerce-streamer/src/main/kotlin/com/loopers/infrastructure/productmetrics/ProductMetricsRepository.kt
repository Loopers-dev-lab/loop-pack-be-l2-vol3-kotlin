package com.loopers.infrastructure.productmetrics

import com.loopers.domain.productmetrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductMetricsRepository : JpaRepository<ProductMetrics, Long> {
    fun findByProductId(productId: Long): ProductMetrics?

    @Modifying(flushAutomatically = true)
    @Query(
        value = "INSERT INTO product_metrics (product_id, view_count, sales_count, updated_at) " +
            "VALUES (:productId, 1, 0, NOW(6)) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + 1, updated_at = NOW(6)",
        nativeQuery = true,
    )
    fun incrementViewCount(@Param("productId") productId: Long)

    @Modifying(flushAutomatically = true)
    @Query(
        value = "INSERT INTO product_metrics (product_id, view_count, sales_count, updated_at) " +
            "VALUES (:productId, 0, :quantity, NOW(6)) " +
            "ON DUPLICATE KEY UPDATE sales_count = sales_count + :quantity, updated_at = NOW(6)",
        nativeQuery = true,
    )
    fun incrementSalesCount(@Param("productId") productId: Long, @Param("quantity") quantity: Int)
}
