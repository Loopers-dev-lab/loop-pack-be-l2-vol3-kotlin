package com.loopers.batch.job.ranking

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@StepScope
@Component
class MonthlyRankingAndTrimTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {

    @Value("#{jobParameters['targetDate']}")
    private lateinit var targetDate: String

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val date = LocalDate.parse(targetDate, DateTimeFormatter.BASIC_ISO_DATE)
        val yearMonth = getYearMonth(date)

        // 누적된 score 기반으로 순위 부여 (window function, MySQL 8.0+)
        jdbcTemplate.update(RANK_SQL, yearMonth)

        // TOP 100 이후 제거
        jdbcTemplate.update(TRIM_SQL, yearMonth)

        return RepeatStatus.FINISHED
    }

    private fun getYearMonth(date: LocalDate): String {
        return String.format("%d-%02d", date.year, date.monthValue)
    }

    companion object {
        private const val RANK_SQL = """
            UPDATE mv_product_rank_monthly m1
            JOIN (
                SELECT id, ROW_NUMBER() OVER (ORDER BY score DESC) AS new_rank
                FROM mv_product_rank_monthly
                WHERE year_month = ?
            ) m2 ON m1.id = m2.id
            SET m1.rank = m2.new_rank, m1.updated_at = NOW()
        """

        private const val TRIM_SQL = """
            DELETE FROM mv_product_rank_monthly
            WHERE year_month = ? AND rank > 100
        """
    }
}
