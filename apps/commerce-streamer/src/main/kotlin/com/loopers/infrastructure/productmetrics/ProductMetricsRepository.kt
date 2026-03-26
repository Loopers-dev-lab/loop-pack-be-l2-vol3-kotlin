package com.loopers.infrastructure.productmetrics

import com.loopers.domain.productmetrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductMetricsRepository : JpaRepository<ProductMetrics, Long> {
    fun findByProductId(productId: Long): ProductMetrics?

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.viewCount = pm.viewCount + 1 WHERE pm.productId = :productId")
    fun incrementViewCount(@Param("productId") productId: Long)

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.salesCount = pm.salesCount + :quantity WHERE pm.productId = :productId")
    fun incrementSalesCount(@Param("productId") productId: Long, @Param("quantity") quantity: Int)
}
