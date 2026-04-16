package com.loopers.batch.job.ranking.weekly

import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.domain.ranking.YearWeek
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Configuration
@EnableConfigurationProperties(RankingWeightProperties::class)
class WeeklyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val rankingWeightProperties: RankingWeightProperties,
    private val dataSource: DataSource,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        private const val CHUNK_STEP_NAME = "weeklyRankingChunkStep"
        private const val SWAP_STEP_NAME = "weeklyRankingSwapStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(weeklyRankingChunkStep(null))
            .next(weeklyRankingSwapStep(null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(CHUNK_STEP_NAME)
    fun weeklyRankingChunkStep(
        @Value("#{jobParameters['targetDate']}") targetDateStr: String?,
    ): Step {
        val targetDate = targetDateStr?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } ?: LocalDate.now()
        val yearWeek = YearWeek.from(targetDate)

        val reader = weeklyRankingReader(dataSource, targetDate, CHUNK_SIZE)
        reader.afterPropertiesSet()

        return StepBuilder(CHUNK_STEP_NAME, jobRepository)
            .chunk<ProductAggregateDto, ProductRankRow>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(WeightScoreProcessor(rankingWeightProperties, yearWeek.toString()))
            .writer(weeklyRankingStagingWriter())
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean
    fun weeklyRankingStagingWriter(): JdbcBatchItemWriter<ProductRankRow> {
        return JdbcBatchItemWriterBuilder<ProductRankRow>()
            .dataSource(dataSource)
            .sql(
                """
                INSERT INTO staging_product_rank_weekly (year_week, product_id, score, rank_num, view_count, like_count, sales_count, updated_at)
                VALUES (:yearWeek, :productId, :score, 0, :viewCount, :likeCount, :salesCount, :updatedAt)
                ON DUPLICATE KEY UPDATE
                    score = VALUES(score), view_count = VALUES(view_count), like_count = VALUES(like_count),
                    sales_count = VALUES(sales_count), updated_at = VALUES(updated_at)
                """.trimIndent(),
            )
            .itemSqlParameterSourceProvider { item ->
                MapSqlParameterSource()
                    .addValue("yearWeek", item.yearWeek)
                    .addValue("productId", item.productId)
                    .addValue("score", item.score)
                    .addValue("viewCount", item.viewCount)
                    .addValue("likeCount", item.likeCount)
                    .addValue("salesCount", item.salesCount)
                    .addValue("updatedAt", LocalDateTime.now())
            }
            .build()
    }

    @JobScope
    @Bean(SWAP_STEP_NAME)
    fun weeklyRankingSwapStep(
        @Value("#{jobParameters['targetDate']}") targetDateStr: String?,
    ): Step {
        val targetDate = targetDateStr?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } ?: LocalDate.now()
        val yearWeek = YearWeek.from(targetDate).toString()

        val swapTasklet = Tasklet { _, _ ->
            val connection = dataSource.connection
            connection.use { conn ->
                conn.autoCommit = false
                conn.createStatement().use { stmt ->
                    // staging에 rank 부여하고 MV로 swap
                    stmt.executeUpdate("DELETE FROM mv_product_rank_weekly WHERE year_week = '$yearWeek'")
                    stmt.executeUpdate(
                        """
                        INSERT INTO mv_product_rank_weekly (year_week, product_id, score, rank_num, view_count, like_count, sales_count, updated_at)
                        SELECT year_week, product_id, score,
                               ROW_NUMBER() OVER (ORDER BY score DESC) AS rank_num,
                               view_count, like_count, sales_count, updated_at
                        FROM staging_product_rank_weekly
                        WHERE year_week = '$yearWeek'
                        """.trimIndent(),
                    )
                    stmt.executeUpdate("DELETE FROM staging_product_rank_weekly WHERE year_week = '$yearWeek'")
                }
                conn.commit()
            }
            RepeatStatus.FINISHED
        }

        return StepBuilder(SWAP_STEP_NAME, jobRepository)
            .tasklet(swapTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
