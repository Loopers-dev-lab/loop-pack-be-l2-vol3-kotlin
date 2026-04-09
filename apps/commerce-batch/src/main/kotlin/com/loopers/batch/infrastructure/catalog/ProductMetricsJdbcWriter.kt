package com.loopers.batch.infrastructure.catalog

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProductMetricsJdbcWriter(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = LoggerFactory.getLogger(ProductMetricsJdbcWriter::class.java)

    companion object {
        // Redis가 SoT이므로 누적 합산이 아닌 스냅샷 덮어쓰기
        private const val UPSERT_SQL = """
            INSERT INTO product_metrics (product_id, view_count, like_count, order_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = VALUES(view_count),
                like_count = VALUES(like_count),
                order_count = VALUES(order_count),
                updated_at = NOW()
        """

        // 스냅샷 동기화 전에 모든 행을 0으로 초기화 (Option A — 향후 (product_id, snapshot_date)
        // 스키마로 전환 예정. 자세한 내용은 docs/pr/w8-product-metrics-sync.md 참고)
        private const val RESET_SQL = """
            UPDATE product_metrics
               SET view_count = 0,
                   like_count = 0,
                   order_count = 0,
                   updated_at = NOW()
        """
    }

    fun upsertAll(snapshots: List<ProductMetricsSnapshot>) {
        if (snapshots.isEmpty()) {
            // 빈 스냅샷으로 DB가 wipe되는 위험 방지: reset 자체를 건너뛴다.
            log.info("동기화할 메트릭이 없습니다. DB 변경을 건너뜁니다.")
            return
        }

        val resetCount = jdbcTemplate.update(RESET_SQL)
        log.info("기존 메트릭 {}개 행을 0으로 초기화했습니다.", resetCount)

        val batchArgs = snapshots.map { snapshot ->
            arrayOf(
                snapshot.productId,
                snapshot.viewCount,
                snapshot.likeCount,
                snapshot.orderCount,
            )
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, batchArgs)
        log.info("{}개 상품 메트릭을 DB에 동기화했습니다.", snapshots.size)
    }
}
