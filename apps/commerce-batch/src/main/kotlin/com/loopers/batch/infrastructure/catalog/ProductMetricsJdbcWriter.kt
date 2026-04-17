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
        // (product_id, date) UNIQUE 인덱스 기반 스냅샷 덮어쓰기
        private const val UPSERT_SQL = """
            INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = VALUES(view_count),
                like_count = VALUES(like_count),
                order_count = VALUES(order_count),
                updated_at = NOW()
        """
    }

    fun upsertAll(snapshots: List<ProductMetricsSnapshot>) {
        if (snapshots.isEmpty()) {
            log.info("동기화할 메트릭이 없습니다. DB 변경을 건너뜁니다.")
            return
        }

        val batchArgs = snapshots.map { snapshot ->
            arrayOf(
                snapshot.productId,
                snapshot.date,
                snapshot.viewCount,
                snapshot.likeCount,
                snapshot.orderCount,
            )
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, batchArgs)
        log.info("{}개 상품 메트릭을 DB에 동기화했습니다.", snapshots.size)
    }
}
