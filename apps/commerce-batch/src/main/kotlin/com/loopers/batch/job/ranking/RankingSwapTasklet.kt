package com.loopers.batch.job.ranking

import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate

fun rankingSwapTasklet(
    jdbcTemplate: JdbcTemplate,
    mvTableName: String,
    stagingTableName: String,
    periodColumnName: String,
    periodKey: String,
): Tasklet {
    return Tasklet { _, _ ->
        jdbcTemplate.update("DELETE FROM $mvTableName WHERE $periodColumnName = ?", periodKey)
        jdbcTemplate.update(
            """
            INSERT INTO $mvTableName ($periodColumnName, product_id, score, rank_num, view_count, like_count, sales_count, updated_at)
            SELECT $periodColumnName, product_id, score,
                   ROW_NUMBER() OVER (ORDER BY score DESC) AS rank_num,
                   view_count, like_count, sales_count, updated_at
            FROM $stagingTableName
            WHERE $periodColumnName = ?
            """.trimIndent(),
            periodKey,
        )
        jdbcTemplate.update("DELETE FROM $stagingTableName WHERE $periodColumnName = ?", periodKey)
        RepeatStatus.FINISHED
    }
}
