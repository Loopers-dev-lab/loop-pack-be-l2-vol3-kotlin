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
import java.time.temporal.IsoFields

@StepScope
@Component
class WeeklyRankingAndTrimTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {

    @Value("#{jobParameters['targetDate']}")
    private lateinit var targetDate: String

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val date = LocalDate.parse(targetDate, DateTimeFormatter.BASIC_ISO_DATE)
        val yearWeek = getYearWeek(date)

        // 누적된 score 기반으로 순위 부여 (window function, MySQL 8.0+)
        jdbcTemplate.update(RANK_SQL, yearWeek)

        // TOP 100 이후 제거
        jdbcTemplate.update(TRIM_SQL, yearWeek)

        return RepeatStatus.FINISHED
    }

    private fun getYearWeek(date: LocalDate): String {
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return String.format("%d-W%02d", date.year, week)
    }

    companion object {
        private const val RANK_SQL = """
            UPDATE mv_product_rank_weekly w1
            JOIN (
                SELECT id, ROW_NUMBER() OVER (ORDER BY score DESC) AS new_rank
                FROM mv_product_rank_weekly
                WHERE year_week = ?
            ) w2 ON w1.id = w2.id
            SET w1.rank = w2.new_rank, w1.updated_at = NOW()
        """

        private const val TRIM_SQL = """
            DELETE FROM mv_product_rank_weekly
            WHERE year_week = ? AND rank > 100
        """
    }
}
