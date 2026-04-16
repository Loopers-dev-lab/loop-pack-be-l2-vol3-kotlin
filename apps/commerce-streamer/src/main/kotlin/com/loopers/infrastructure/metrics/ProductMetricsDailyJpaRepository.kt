package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsDaily
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ProductMetricsDailyJpaRepository : JpaRepository<ProductMetricsDaily, Long> {

    fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetricsDaily?

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics_daily (metric_date, product_id, view_count, like_count, sales_count)
        VALUES (:metricDate, :productId, 1, 0, 0)
        ON DUPLICATE KEY UPDATE
            view_count = view_count + 1
        """,
        nativeQuery = true,
    )
    fun incrementViewCount(productId: Long, metricDate: LocalDate)

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics_daily (metric_date, product_id, view_count, like_count, sales_count)
        VALUES (:metricDate, :productId, 0, 1, 0)
        ON DUPLICATE KEY UPDATE
            like_count = like_count + 1
        """,
        nativeQuery = true,
    )
    fun incrementLikeCount(productId: Long, metricDate: LocalDate)

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics_daily (metric_date, product_id, view_count, like_count, sales_count)
        VALUES (:metricDate, :productId, 0, 0, 0)
        ON DUPLICATE KEY UPDATE
            like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END
        """,
        nativeQuery = true,
    )
    fun decrementLikeCount(productId: Long, metricDate: LocalDate)

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics_daily (metric_date, product_id, view_count, like_count, sales_count)
        VALUES (:metricDate, :productId, 0, 0, :quantity)
        ON DUPLICATE KEY UPDATE
            sales_count = sales_count + :quantity
        """,
        nativeQuery = true,
    )
    fun incrementSalesCount(productId: Long, metricDate: LocalDate, quantity: Int)
}
