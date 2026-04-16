package com.loopers.infrastructure.productmetrics

import com.loopers.domain.productmetrics.ProductMetricsDaily
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ProductMetricsDailyRepository : JpaRepository<ProductMetricsDaily, Long> {

    @Modifying
    @Query(
        value = "INSERT INTO product_metrics_daily (product_id, view_count, sales_count, like_count, metric_date, created_at) " +
            "VALUES (:productId, 1, 0, 0, :metricDate, NOW()) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + 1",
        nativeQuery = true,
    )
    fun incrementViewCount(
        @Param("productId") productId: Long,
        @Param("metricDate") metricDate: LocalDate,
    )

    @Modifying
    @Query(
        value = "INSERT INTO product_metrics_daily (product_id, view_count, sales_count, like_count, metric_date, created_at) " +
            "VALUES (:productId, 0, :quantity, 0, :metricDate, NOW()) " +
            "ON DUPLICATE KEY UPDATE sales_count = sales_count + VALUES(sales_count)",
        nativeQuery = true,
    )
    fun incrementSalesCount(
        @Param("productId") productId: Long,
        @Param("quantity") quantity: Long,
        @Param("metricDate") metricDate: LocalDate,
    )

    @Modifying
    @Query(
        value = "INSERT INTO product_metrics_daily (product_id, view_count, sales_count, like_count, metric_date, created_at) " +
            "VALUES (:productId, 0, 0, :delta, :metricDate, NOW()) " +
            "ON DUPLICATE KEY UPDATE like_count = GREATEST(like_count + VALUES(like_count), 0)",
        nativeQuery = true,
    )
    fun incrementLikeCount(
        @Param("productId") productId: Long,
        @Param("delta") delta: Long,
        @Param("metricDate") metricDate: LocalDate,
    )
}
