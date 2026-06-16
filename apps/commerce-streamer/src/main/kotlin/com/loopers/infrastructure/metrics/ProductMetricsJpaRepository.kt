package com.loopers.infrastructure.metrics

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ProductMetricsJpaRepository : JpaRepository<ProductMetricsEntity, Long> {

    @Modifying
    @Query(
        value = """
            INSERT INTO product_metrics (product_id, bucket_date, view_count, like_count, order_count, sales_amount, created_at, updated_at)
            VALUES (:productId, :bucketDate, :viewCount, :likeCount, :orderCount, :salesAmount, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = view_count + :viewCount,
                like_count = GREATEST(like_count + :likeCount, 0),
                order_count = order_count + :orderCount,
                sales_amount = sales_amount + :salesAmount,
                updated_at = NOW()
        """,
        nativeQuery = true,
    )
    fun upsertMetrics(
        productId: Long,
        bucketDate: LocalDate,
        viewCount: Long,
        likeCount: Long,
        orderCount: Long,
        salesAmount: Long,
    )
}
