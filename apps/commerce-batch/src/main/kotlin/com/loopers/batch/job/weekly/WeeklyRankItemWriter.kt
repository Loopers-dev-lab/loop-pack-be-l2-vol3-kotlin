package com.loopers.batch.job.weekly

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.ZonedDateTime

@Component
class WeeklyRankItemWriter(
    private val jdbcTemplate: JdbcTemplate,
) : ItemWriter<RankedWeekly> {

    override fun write(chunk: Chunk<out RankedWeekly>) {
        if (chunk.isEmpty) return
        val now = Timestamp.from(ZonedDateTime.now().toInstant())
        jdbcTemplate.batchUpdate(
            UPSERT_SQL,
            chunk.items.map { item ->
                arrayOf<Any?>(
                    item.productId,
                    java.sql.Date.valueOf(item.weekEnd),
                    java.sql.Date.valueOf(item.weekStart),
                    item.viewCount,
                    item.likeCount,
                    item.orderCount,
                    item.totalScore,
                    item.rankPosition,
                    now,
                )
            },
        )
    }

    companion object {
        private const val UPSERT_SQL = """
            INSERT INTO mv_product_rank_weekly
              (product_id, week_end, week_start, view_count, like_count, order_count,
               total_score, rank_position, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              week_start    = VALUES(week_start),
              view_count    = VALUES(view_count),
              like_count    = VALUES(like_count),
              order_count   = VALUES(order_count),
              total_score   = VALUES(total_score),
              rank_position = VALUES(rank_position),
              updated_at    = VALUES(updated_at)
        """
    }
}
