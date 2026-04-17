package com.loopers.batch.job.weekly

import com.loopers.batch.listener.StepMonitorListener
import com.loopers.batch.ranking.RankingWeightProperties
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.TransientDataAccessException
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import javax.sql.DataSource

@Configuration
class WeeklyRankJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val dataSource: DataSource,
    private val jobListener: WeeklyRankJobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val itemWriter: WeeklyRankItemWriter,
    private val properties: WeeklyRankProperties,
    private val weight: RankingWeightProperties,
) {

    @Bean(JOB_NAME)
    fun weeklyRankJob(@Qualifier(STEP_NAME) step: Step): Job = JobBuilder(JOB_NAME, jobRepository)
        .start(step)
        .listener(jobListener)
        .build()

    @JobScope
    @Bean(STEP_NAME)
    fun weeklyRankStep(@Qualifier(READER_NAME) reader: ItemReader<RankedWeekly>): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .chunk<RankedWeekly, RankedWeekly>(properties.chunkSize, transactionManager)
            .reader(reader)
            .writer(itemWriter)
            .faultTolerant()
            .skip(NumberFormatException::class.java)
            .skipLimit(properties.skipLimit)
            .retry(TransientDataAccessException::class.java)
            .retryLimit(properties.retryLimit)
            .listener(stepMonitorListener)
            .build()

    @StepScope
    @Bean(READER_NAME)
    fun weeklyRankReader(
        @Value("#{jobParameters['targetDate']}") targetDate: LocalDate,
    ): JdbcCursorItemReader<RankedWeekly> {
        val weekStart = targetDate.minusDays((WeeklyRankJobListener.WEEK_SPAN_DAYS - 1).toLong())
        return JdbcCursorItemReaderBuilder<RankedWeekly>()
            .name(READER_NAME)
            .dataSource(dataSource)
            .sql(AGGREGATE_SQL)
            .preparedStatementSetter { ps ->
                ps.setDouble(1, weight.view)
                ps.setDouble(2, weight.like)
                ps.setDouble(3, weight.order)
                ps.setObject(4, weekStart)
                ps.setObject(5, targetDate)
                ps.setInt(6, properties.topN)
            }
            .rowMapper { rs, _ ->
                RankedWeekly(
                    productId = rs.getLong("product_id"),
                    weekStart = weekStart,
                    weekEnd = targetDate,
                    viewCount = rs.getLong("view_sum"),
                    likeCount = rs.getLong("like_sum"),
                    orderCount = rs.getLong("order_sum"),
                    totalScore = rs.getDouble("total_score"),
                    rankPosition = rs.getInt("rank_position"),
                )
            }
            .build()
    }

    companion object {
        const val JOB_NAME = "weeklyRankJob"
        const val STEP_NAME = "weeklyRankStep"
        const val READER_NAME = "weeklyRankReader"

        private const val AGGREGATE_SQL = """
            WITH aggregated AS (
                SELECT product_id,
                       SUM(view_count)  AS view_sum,
                       SUM(like_count)  AS like_sum,
                       SUM(order_count) AS order_sum,
                       (SUM(view_count) * ? + SUM(like_count) * ? + SUM(order_count) * ?) AS total_score
                FROM product_metrics_daily
                WHERE metric_date BETWEEN ? AND ?
                GROUP BY product_id
            )
            SELECT product_id, view_sum, like_sum, order_sum, total_score,
                   ROW_NUMBER() OVER (ORDER BY total_score DESC) AS rank_position
            FROM aggregated
            ORDER BY rank_position
            LIMIT ?
        """
    }
}
