package com.loopers.batch.job.snapshot

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.ZonedDateTime

@Component
class DailySnapshotItemWriter(
    private val jdbcTemplate: JdbcTemplate,
) : ItemWriter<RankedSnapshot> {

    override fun write(chunk: Chunk<out RankedSnapshot>) {
        if (chunk.isEmpty) return
        val now = Timestamp.from(ZonedDateTime.now().toInstant())
        jdbcTemplate.batchUpdate(
            UPSERT_SQL,
            chunk.items.map { item ->
                arrayOf<Any?>(
                    item.productId,
                    java.sql.Date.valueOf(item.metricDate),
                    0L,
                    0L,
                    0L,
                    item.totalScore,
                    item.rankPosition,
                    now,
                )
            },
        )
    }

    companion object {
        private const val UPSERT_SQL = """
            INSERT INTO product_metrics_daily
              (product_id, metric_date, view_count, like_count, order_count,
               total_score, rank_position, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              view_count    = VALUES(view_count),
              like_count    = VALUES(like_count),
              order_count   = VALUES(order_count),
              total_score   = VALUES(total_score),
              rank_position = VALUES(rank_position),
              updated_at    = VALUES(updated_at)
        """
    }
}
