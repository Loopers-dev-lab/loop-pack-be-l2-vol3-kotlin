package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.weekly.ProductAggregateDto
import com.loopers.batch.job.ranking.weekly.RankingWeightProperties
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
@EnableConfigurationProperties(RankingWeightProperties::class)
class MonthlyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val rankingWeightProperties: RankingWeightProperties,
    private val dataSource: DataSource,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val CHUNK_STEP_NAME = "monthlyRankingChunkStep"
        private const val SWAP_STEP_NAME = "monthlyRankingSwapStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(JOB_NAME)
    fun monthlyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(monthlyRankingChunkStep(null))
            .next(monthlyRankingSwapStep(null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(CHUNK_STEP_NAME)
    fun monthlyRankingChunkStep(
        @Value("#{jobParameters['targetDate']}") targetDateStr: String?,
    ): Step {
        val targetDate = targetDateStr?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } ?: LocalDate.now()
        val yearMonth = YearMonth.from(targetDate).toString()

        val reader = monthlyRankingReader(dataSource, targetDate, CHUNK_SIZE)
        reader.afterPropertiesSet()

        return StepBuilder(CHUNK_STEP_NAME, jobRepository)
            .chunk<ProductAggregateDto, ProductRankMonthlyRow>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(MonthlyWeightScoreProcessor(rankingWeightProperties, yearMonth))
            .writer(monthlyRankingStagingWriter())
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean
    fun monthlyRankingStagingWriter(): JdbcBatchItemWriter<ProductRankMonthlyRow> {
        return JdbcBatchItemWriterBuilder<ProductRankMonthlyRow>()
            .dataSource(dataSource)
            .sql(
                """
                INSERT INTO staging_product_rank_monthly (`year_month`, product_id, score, rank_num, view_count, like_count, sales_count, updated_at)
                VALUES (:yearMonth, :productId, :score, 0, :viewCount, :likeCount, :salesCount, :updatedAt)
                ON DUPLICATE KEY UPDATE
                    score = VALUES(score), view_count = VALUES(view_count), like_count = VALUES(like_count),
                    sales_count = VALUES(sales_count), updated_at = VALUES(updated_at)
                """.trimIndent(),
            )
            .itemSqlParameterSourceProvider { item ->
                MapSqlParameterSource()
                    .addValue("yearMonth", item.yearMonth)
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
    fun monthlyRankingSwapStep(
        @Value("#{jobParameters['targetDate']}") targetDateStr: String?,
    ): Step {
        val targetDate = targetDateStr?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } ?: LocalDate.now()
        val yearMonth = YearMonth.from(targetDate).toString()

        val swapTasklet = Tasklet { _, _ ->
            val connection = dataSource.connection
            connection.use { conn ->
                conn.autoCommit = false
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM mv_product_rank_monthly WHERE `year_month` = '$yearMonth'")
                    stmt.executeUpdate(
                        """
                        INSERT INTO mv_product_rank_monthly (`year_month`, product_id, score, rank_num, view_count, like_count, sales_count, updated_at)
                        SELECT `year_month`, product_id, score,
                               ROW_NUMBER() OVER (ORDER BY score DESC) AS rank_num,
                               view_count, like_count, sales_count, updated_at
                        FROM staging_product_rank_monthly
                        WHERE `year_month` = '$yearMonth'
                        """.trimIndent(),
                    )
                    stmt.executeUpdate("DELETE FROM staging_product_rank_monthly WHERE `year_month` = '$yearMonth'")
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
