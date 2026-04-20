package com.loopers.batch.job.ranking.aggregate.step

import com.loopers.batch.job.ranking.aggregate.RankingPeriod
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@StepScope
class SortAndAssignRankTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val stepExecution = chunkContext.stepContext.stepExecution
        val jobExecutionId = stepExecution.jobExecutionId
        val jobParameters = stepExecution.jobParameters
        val periodKey = jobParameters.getString("periodKey")
            ?: error("JobParameter 'periodKey' is required")
        val periodStr = jobParameters.getString("period")
            ?: error("JobParameter 'period' is required")
        val period = RankingPeriod.valueOf(periodStr)

        val tableName = when (period) {
            RankingPeriod.WEEKLY -> "mv_product_rank_weekly"
            RankingPeriod.MONTHLY -> "mv_product_rank_monthly"
        }

        log.info("[SortAndAssignRankTasklet] 시작: table={}, periodKey={}, jobExecutionId={}", tableName, periodKey, jobExecutionId)

        // 기존 MV 테이블의 period_key row 삭제 (빈 기간에도 잔재 방지)
        val deletedMv = jdbcTemplate.update(
            "DELETE FROM $tableName WHERE period_key = ?",
            periodKey,
        )
        log.info("[SortAndAssignRankTasklet] 기존 MV 삭제: {}건", deletedMv)

        // staging에서 상위 200개 조회
        val rows = jdbcTemplate.query(
            """
            SELECT product_id, score, view_count, like_count, order_count, order_amount_sum
            FROM rank_staging
            WHERE job_execution_id = ?
            ORDER BY score DESC, product_id ASC
            LIMIT 200
            """.trimIndent(),
            { rs, _ ->
                arrayOf(
                    rs.getLong("product_id"),
                    rs.getDouble("score"),
                    rs.getLong("view_count"),
                    rs.getLong("like_count"),
                    rs.getLong("order_count"),
                    rs.getLong("order_amount_sum"),
                )
            },
            jobExecutionId,
        )

        if (rows.isEmpty()) {
            log.info("[SortAndAssignRankTasklet] staging 결과 없음, MV 적재 스킵")
        } else {
            val sql = """
                INSERT INTO $tableName
                    (period_key, product_id, rank_value, score, view_count, like_count, order_count, order_amount_sum, computed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(6))
                ON DUPLICATE KEY UPDATE
                    rank_value = VALUES(rank_value),
                    score = VALUES(score),
                    view_count = VALUES(view_count),
                    like_count = VALUES(like_count),
                    order_count = VALUES(order_count),
                    order_amount_sum = VALUES(order_amount_sum),
                    computed_at = NOW(6)
            """.trimIndent()

            val rankedRows = rows.mapIndexed { index, row -> Pair(index + 1, row) }
            jdbcTemplate.batchUpdate(
                sql,
                rankedRows,
                rankedRows.size,
            ) { ps, (rank, row) ->
                ps.setString(1, periodKey)
                ps.setLong(2, row[0] as Long)
                ps.setInt(3, rank)
                ps.setDouble(4, row[1] as Double)
                ps.setLong(5, row[2] as Long)
                ps.setLong(6, row[3] as Long)
                ps.setLong(7, row[4] as Long)
                ps.setLong(8, row[5] as Long)
            }
            log.info("[SortAndAssignRankTasklet] MV 적재 완료: {}건", rows.size)
        }

        // staging cleanup
        val deletedStaging = jdbcTemplate.update(
            "DELETE FROM rank_staging WHERE job_execution_id = ?",
            jobExecutionId,
        )
        log.info("[SortAndAssignRankTasklet] staging 정리 완료: {}건", deletedStaging)

        return RepeatStatus.FINISHED
    }
}
