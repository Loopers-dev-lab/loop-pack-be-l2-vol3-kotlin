package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsDailyModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ProductMetricsDailyJpaRepository : JpaRepository<ProductMetricsDailyModel, Long> {
    /**
     * (product_id, metric_date) 유니크 키 기준으로 delta 를 누적 upsert 한다.
     *
     * - `registerEvent` + `isStale` 가드를 통과한 이벤트만 본 쿼리에 도달하므로,
     *   중복·역전 방어는 상위 계층이 책임진다.
     * - MySQL 전용 `INSERT ... ON DUPLICATE KEY UPDATE` 구문을 사용한다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO product_metrics_daily
                (product_id, metric_date, likes_count, views_count, sales_count, created_at, updated_at)
            VALUES
                (:productId, :metricDate, :likesDelta, :viewsDelta, :salesDelta, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE
                likes_count = likes_count + VALUES(likes_count),
                views_count = views_count + VALUES(views_count),
                sales_count = sales_count + VALUES(sales_count),
                updated_at  = NOW(6)
        """,
        nativeQuery = true,
    )
    fun upsert(
        @Param("productId") productId: Long,
        @Param("metricDate") metricDate: LocalDate,
        @Param("likesDelta") likesDelta: Long,
        @Param("viewsDelta") viewsDelta: Long,
        @Param("salesDelta") salesDelta: Long,
    ): Int

    fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetricsDailyModel?
}
