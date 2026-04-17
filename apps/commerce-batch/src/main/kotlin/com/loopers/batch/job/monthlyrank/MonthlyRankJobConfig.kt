package com.loopers.batch.job.monthlyrank

import com.loopers.batch.job.monthlyrank.step.MonthlyRankDeleteTasklet
import com.loopers.batch.job.weeklyrank.RankRow
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankJobConfig.JOB_NAME)
@Configuration
class MonthlyRankJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val monthlyRankDeleteTasklet: MonthlyRankDeleteTasklet,
    private val dataSource: DataSource,
) {
    companion object {
        const val JOB_NAME = "monthlyRankJob"
        private const val DELETE_STEP_NAME = "monthlyRankDeleteStep"
        private const val CHUNK_STEP_NAME = "monthlyRankChunkStep"
        private const val CHUNK_SIZE = 100

        private const val READER_SQL = """
            SELECT product_id,
                   SUM(view_count) * 0.1 + SUM(like_count) * 0.2 + SUM(order_count) * 0.7 AS score
            FROM product_metrics
            WHERE date BETWEEN ? AND ?
            GROUP BY product_id
            ORDER BY score DESC
            LIMIT 100
        """

        private const val WRITER_SQL = """
            INSERT INTO mv_product_rank_monthly (product_id, score, created_at, updated_at)
            VALUES (?, ?, NOW(), NOW())
        """
    }

    @Bean(JOB_NAME)
    fun monthlyRankJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(monthlyRankDeleteStep())
            .next(monthlyRankChunkStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(DELETE_STEP_NAME)
    fun monthlyRankDeleteStep(): Step {
        return StepBuilder(DELETE_STEP_NAME, jobRepository)
            .tasklet(monthlyRankDeleteTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(CHUNK_STEP_NAME)
    fun monthlyRankChunkStep(): Step {
        return StepBuilder(CHUNK_STEP_NAME, jobRepository)
            .chunk<RankRow, RankRow>(CHUNK_SIZE, transactionManager)
            .reader(monthlyRankReader())
            .writer(monthlyRankWriter())
            .listener(stepMonitorListener)
            .build()
    }

    @Bean
    fun monthlyRankReader(): JdbcCursorItemReader<RankRow> {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)

        return JdbcCursorItemReaderBuilder<RankRow>()
            .name("monthlyRankReader")
            .dataSource(dataSource)
            .sql(READER_SQL)
            .preparedStatementSetter { ps ->
                ps.setObject(1, startOfMonth)
                ps.setObject(2, today)
            }
            .rowMapper { rs: ResultSet, _: Int ->
                RankRow(
                    productId = rs.getLong("product_id"),
                    score = rs.getDouble("score"),
                )
            }
            .build()
    }

    @Bean
    fun monthlyRankWriter(): JdbcBatchItemWriter<RankRow> {
        return JdbcBatchItemWriterBuilder<RankRow>()
            .dataSource(dataSource)
            .sql(WRITER_SQL)
            .itemPreparedStatementSetter { item, ps ->
                ps.setLong(1, item.productId)
                ps.setDouble(2, item.score)
            }
            .build()
    }
}
