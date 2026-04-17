package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.RankingAggregationProcessor
import com.loopers.batch.job.ranking.step.RankingAggregationReader
import com.loopers.batch.job.ranking.step.RankingAggregationWriter
import com.loopers.batch.job.ranking.step.RankingCleanupTasklet
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.domain.ranking.ProductRankAggregation
import com.loopers.domain.ranking.ProductRankMonthlyRepository
import com.loopers.domain.ranking.ProductRankResult
import com.loopers.domain.ranking.ProductRankWeeklyRepository
import com.loopers.domain.ranking.RankingPeriodType
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = RankingAggregationJobConfig.JOB_NAME)
@Configuration
class RankingAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
) {
    companion object {
        const val JOB_NAME = "rankingAggregationJob"
        private const val CHUNK_SIZE = 500
        private const val FETCH_SIZE = 500
        private const val TOP_N = 100

        private const val DEFAULT_VIEW_WEIGHT = 0.1
        private const val DEFAULT_LIKE_WEIGHT = 0.2
        private const val DEFAULT_ORDER_WEIGHT = 0.6

        private const val WEIGHT_SQL = "SELECT config_key, config_value FROM ranking_score_config"
    }

    // --- Job ---

    @Bean(JOB_NAME)
    fun rankingAggregationJob(
        @Qualifier("weeklyCleanupStep") weeklyCleanupStep: Step,
        @Qualifier("weeklyAggregationStep") weeklyAggregationStep: Step,
        @Qualifier("monthlyCleanupStep") monthlyCleanupStep: Step,
        @Qualifier("monthlyAggregationStep") monthlyAggregationStep: Step,
    ): Job {
        val weeklyFlow = FlowBuilder<Flow>("weeklyFlow")
            .start(weeklyCleanupStep)
            .next(weeklyAggregationStep)
            .build()

        val monthlyFlow = FlowBuilder<Flow>("monthlyFlow")
            .start(monthlyCleanupStep)
            .next(monthlyAggregationStep)
            .build()

        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .listener(jobListener)
            .start(weeklyFlow)
            .split(SimpleAsyncTaskExecutor())
            .add(monthlyFlow)
            .end()
            .build()
    }

    // --- Steps ---

    @Bean("weeklyCleanupStep")
    fun weeklyCleanupStep(@Qualifier("weeklyCleanupTasklet") tasklet: Tasklet): Step =
        buildTaskletStep("weeklyCleanupStep", tasklet)

    @Bean("weeklyAggregationStep")
    fun weeklyAggregationStep(
        @Qualifier("weeklyReader") reader: ItemReader<ProductRankAggregation>,
        @Qualifier("weeklyProcessor") processor: ItemProcessor<ProductRankAggregation, ProductRankResult>,
        @Qualifier("weeklyWriter") writer: ItemWriter<ProductRankResult>,
    ): Step = buildChunkStep("weeklyAggregationStep", reader, processor, writer)

    @Bean("monthlyCleanupStep")
    fun monthlyCleanupStep(@Qualifier("monthlyCleanupTasklet") tasklet: Tasklet): Step =
        buildTaskletStep("monthlyCleanupStep", tasklet)

    @Bean("monthlyAggregationStep")
    fun monthlyAggregationStep(
        @Qualifier("monthlyReader") reader: ItemReader<ProductRankAggregation>,
        @Qualifier("monthlyProcessor") processor: ItemProcessor<ProductRankAggregation, ProductRankResult>,
        @Qualifier("monthlyWriter") writer: ItemWriter<ProductRankResult>,
    ): Step = buildChunkStep("monthlyAggregationStep", reader, processor, writer)

    // --- @StepScope Beans (Weekly) ---

    @StepScope
    @Bean("weeklyCleanupTasklet")
    fun weeklyCleanupTasklet(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        weeklyRepository: ProductRankWeeklyRepository,
    ): Tasklet {
        val date = LocalDate.parse(requestDate)
        return RankingCleanupTasklet(
            "WEEKLY",
            RankingPeriodType.WEEKLY.periodStartDate(date),
            weeklyRepository::deleteByPeriodStartDate,
        )
    }

    @StepScope
    @Bean("weeklyReader")
    fun weeklyReader(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        dataSource: DataSource,
        jdbcTemplate: NamedParameterJdbcTemplate,
    ): JdbcCursorItemReader<ProductRankAggregation> {
        val date = LocalDate.parse(requestDate)
        val periodType = RankingPeriodType.WEEKLY
        return RankingAggregationReader.create(
            name = "weeklyReader",
            dataSource = dataSource,
            startDate = periodType.periodStartDate(date),
            endDate = periodType.periodEndDate(date),
            weights = loadWeights(jdbcTemplate),
            topN = TOP_N,
            fetchSize = FETCH_SIZE,
        )
    }

    @StepScope
    @Bean("weeklyProcessor")
    fun weeklyProcessor(): ItemProcessor<ProductRankAggregation, ProductRankResult> = RankingAggregationProcessor()

    @StepScope
    @Bean("weeklyWriter")
    fun weeklyWriter(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        weeklyRepository: ProductRankWeeklyRepository,
    ): ItemWriter<ProductRankResult> {
        val date = LocalDate.parse(requestDate)
        val periodType = RankingPeriodType.WEEKLY
        return RankingAggregationWriter(
            weeklyRepository::batchInsert,
            periodType.periodStartDate(date),
            periodType.periodEndDate(date),
        )
    }

    // --- @StepScope Beans (Monthly) ---

    @StepScope
    @Bean("monthlyCleanupTasklet")
    fun monthlyCleanupTasklet(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        monthlyRepository: ProductRankMonthlyRepository,
    ): Tasklet {
        val date = LocalDate.parse(requestDate)
        return RankingCleanupTasklet(
            "MONTHLY",
            RankingPeriodType.MONTHLY.periodStartDate(date),
            monthlyRepository::deleteByPeriodStartDate,
        )
    }

    @StepScope
    @Bean("monthlyReader")
    fun monthlyReader(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        dataSource: DataSource,
        jdbcTemplate: NamedParameterJdbcTemplate,
    ): JdbcCursorItemReader<ProductRankAggregation> {
        val date = LocalDate.parse(requestDate)
        val periodType = RankingPeriodType.MONTHLY
        return RankingAggregationReader.create(
            name = "monthlyReader",
            dataSource = dataSource,
            startDate = periodType.periodStartDate(date),
            endDate = periodType.periodEndDate(date),
            weights = loadWeights(jdbcTemplate),
            topN = TOP_N,
            fetchSize = FETCH_SIZE,
        )
    }

    @StepScope
    @Bean("monthlyProcessor")
    fun monthlyProcessor(): ItemProcessor<ProductRankAggregation, ProductRankResult> = RankingAggregationProcessor()

    @StepScope
    @Bean("monthlyWriter")
    fun monthlyWriter(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        monthlyRepository: ProductRankMonthlyRepository,
    ): ItemWriter<ProductRankResult> {
        val date = LocalDate.parse(requestDate)
        val periodType = RankingPeriodType.MONTHLY
        return RankingAggregationWriter(
            monthlyRepository::batchInsert,
            periodType.periodStartDate(date),
            periodType.periodEndDate(date),
        )
    }

    // --- Helper ---

    private fun buildTaskletStep(stepName: String, tasklet: Tasklet): Step =
        StepBuilder(stepName, jobRepository)
            .tasklet(tasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()

    private fun buildChunkStep(
        stepName: String,
        reader: ItemReader<ProductRankAggregation>,
        processor: ItemProcessor<ProductRankAggregation, ProductRankResult>,
        writer: ItemWriter<ProductRankResult>,
    ): Step =
        StepBuilder(stepName, jobRepository)
            .chunk<ProductRankAggregation, ProductRankResult>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()

    private fun loadWeights(jdbcTemplate: NamedParameterJdbcTemplate): RankingAggregationReader.ScoreWeights {
        val configMap = jdbcTemplate.query(WEIGHT_SQL, MapSqlParameterSource()) { rs, _ ->
            rs.getString("config_key") to rs.getDouble("config_value")
        }.toMap()

        return RankingAggregationReader.ScoreWeights(
            viewWeight = configMap["VIEW_WEIGHT"] ?: DEFAULT_VIEW_WEIGHT,
            likeWeight = configMap["LIKE_WEIGHT"] ?: DEFAULT_LIKE_WEIGHT,
            orderWeight = configMap["ORDER_WEIGHT"] ?: DEFAULT_ORDER_WEIGHT,
        )
    }
}
