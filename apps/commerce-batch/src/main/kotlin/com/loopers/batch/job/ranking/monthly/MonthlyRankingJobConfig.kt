package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.ProductAggregateDto
import com.loopers.batch.job.ranking.ProductRankRow
import com.loopers.batch.job.ranking.RankingWeightProperties
import com.loopers.batch.job.ranking.WeightScoreProcessor
import com.loopers.batch.job.ranking.parseTargetDate
import com.loopers.batch.job.ranking.rankingStagingWriter
import com.loopers.batch.job.ranking.rankingSwapTasklet
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
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.time.YearMonth
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
    private val jdbcTemplate: JdbcTemplate,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val CHUNK_STEP_NAME = "monthlyRankingChunkStep"
        private const val SWAP_STEP_NAME = "monthlyRankingSwapStep"
        private const val CHUNK_SIZE = 1000
        private const val MV_TABLE = "mv_product_rank_monthly"
        private const val STAGING_TABLE = "staging_product_rank_monthly"
        private const val PERIOD_COLUMN = "`year_month`"
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
        val targetDate = parseTargetDate(targetDateStr)
        val yearMonth = YearMonth.from(targetDate).toString()

        val reader = monthlyRankingReader(dataSource, targetDate, CHUNK_SIZE)
        reader.afterPropertiesSet()

        return StepBuilder(CHUNK_STEP_NAME, jobRepository)
            .chunk<ProductAggregateDto, ProductRankRow>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(WeightScoreProcessor(rankingWeightProperties, yearMonth))
            .writer(monthlyRankingStagingWriter())
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean
    fun monthlyRankingStagingWriter(): JdbcBatchItemWriter<ProductRankRow> {
        return rankingStagingWriter(dataSource, STAGING_TABLE, PERIOD_COLUMN)
    }

    @JobScope
    @Bean(SWAP_STEP_NAME)
    fun monthlyRankingSwapStep(
        @Value("#{jobParameters['targetDate']}") targetDateStr: String?,
    ): Step {
        val targetDate = parseTargetDate(targetDateStr)
        val yearMonth = YearMonth.from(targetDate).toString()

        return StepBuilder(SWAP_STEP_NAME, jobRepository)
            .tasklet(rankingSwapTasklet(jdbcTemplate, MV_TABLE, STAGING_TABLE, PERIOD_COLUMN, yearMonth), transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
