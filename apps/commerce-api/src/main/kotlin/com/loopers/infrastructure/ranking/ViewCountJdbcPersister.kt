package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.ViewCountPersister
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class ViewCountJdbcPersister(
    private val jdbcTemplate: JdbcTemplate,
) : ViewCountPersister {

    override fun incrementViewCounts(viewCounts: Map<Long, Long>) {
        if (viewCounts.isEmpty()) return

        val sql = """
            INSERT INTO product_metrics (product_id, view_count, like_count, order_count, total_revenue, version, created_at, updated_at)
            VALUES (?, ?, 0, 0, 0, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE view_count = view_count + VALUES(view_count), updated_at = NOW()
        """.trimIndent()

        jdbcTemplate.batchUpdate(sql, viewCounts.entries.toList(), viewCounts.size) { ps, entry ->
            ps.setLong(1, entry.key)
            ps.setLong(2, entry.value)
        }
    }
}
