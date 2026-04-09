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
    }

    fun upsertAll(metrics: Map<Long, Map<String, Long>>) {
        if (metrics.isEmpty()) {
            log.info("동기화할 메트릭이 없습니다.")
            return
        }

        val batchArgs = metrics.map { (productId, fields) ->
            arrayOf(
                productId,
                fields["viewCount"] ?: 0L,
                fields["likeCount"] ?: 0L,
                fields["orderCount"] ?: 0L,
            )
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, batchArgs)
        log.info("{}개 상품 메트릭을 DB에 동기화했습니다.", metrics.size)
    }
}
