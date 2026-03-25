package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductMetricsJpaRepository : JpaRepository<ProductMetrics, Long> {

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, version, updated_at)
        VALUES (:productId, 1, 0, 0, :version, NOW())
        ON DUPLICATE KEY UPDATE
            like_count = like_count + 1,
            version = :version,
            updated_at = NOW()
        """,
        nativeQuery = true,
    )
    fun incrementLikeCount(productId: Long, version: Long)

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, version, updated_at)
        VALUES (:productId, 0, 0, 0, :version, NOW())
        ON DUPLICATE KEY UPDATE
            like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END,
            version = :version,
            updated_at = NOW()
        """,
        nativeQuery = true,
    )
    fun decrementLikeCount(productId: Long, version: Long)

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, version, updated_at)
        VALUES (:productId, 0, :quantity, 0, :version, NOW())
        ON DUPLICATE KEY UPDATE
            sales_count = sales_count + :quantity,
            version = :version,
            updated_at = NOW()
        """,
        nativeQuery = true,
    )
    fun incrementSalesCount(productId: Long, quantity: Int, version: Long)

    @Modifying
    @Query(
        """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, version, updated_at)
        VALUES (:productId, 0, 0, 1, :version, NOW())
        ON DUPLICATE KEY UPDATE
            view_count = view_count + 1,
            version = :version,
            updated_at = NOW()
        """,
        nativeQuery = true,
    )
    fun incrementViewCount(productId: Long, version: Long)

}
