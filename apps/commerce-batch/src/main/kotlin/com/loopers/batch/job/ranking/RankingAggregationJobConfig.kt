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
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

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
        private const val CHUNK_SIZE = 100
    }

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

    // --- Weekly Steps ---

    @Bean("weeklyCleanupStep")
    fun weeklyCleanupStep(
        @Qualifier("weeklyCleanupTasklet") tasklet: Tasklet,
    ): Step =
        StepBuilder("weeklyCleanupStep", jobRepository)
            .tasklet(tasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()

    @Bean("weeklyAggregationStep")
    fun weeklyAggregationStep(
        @Qualifier("weeklyReader") reader: ItemReader<ProductRankAggregation>,
        @Qualifier("weeklyProcessor") processor: ItemProcessor<ProductRankAggregation, ProductRankResult>,
        @Qualifier("weeklyWriter") writer: ItemWriter<ProductRankResult>,
    ): Step =
        StepBuilder("weeklyAggregationStep", jobRepository)
            .chunk<ProductRankAggregation, ProductRankResult>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()

    // --- Monthly Steps ---

    @Bean("monthlyCleanupStep")
    fun monthlyCleanupStep(
        @Qualifier("monthlyCleanupTasklet") tasklet: Tasklet,
    ): Step =
        StepBuilder("monthlyCleanupStep", jobRepository)
            .tasklet(tasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()

    @Bean("monthlyAggregationStep")
    fun monthlyAggregationStep(
        @Qualifier("monthlyReader") reader: ItemReader<ProductRankAggregation>,
        @Qualifier("monthlyProcessor") processor: ItemProcessor<ProductRankAggregation, ProductRankResult>,
        @Qualifier("monthlyWriter") writer: ItemWriter<ProductRankResult>,
    ): Step =
        StepBuilder("monthlyAggregationStep", jobRepository)
            .chunk<ProductRankAggregation, ProductRankResult>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()

    // --- @StepScope Beans (Weekly) ---

    @StepScope
    @Bean("weeklyCleanupTasklet")
    fun weeklyCleanupTasklet(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        weeklyRepository: ProductRankWeeklyRepository,
        monthlyRepository: ProductRankMonthlyRepository,
    ): Tasklet =
        RankingCleanupTasklet(RankingPeriodType.WEEKLY, LocalDate.parse(requestDate), weeklyRepository, monthlyRepository)

    @StepScope
    @Bean("weeklyReader")
    fun weeklyReader(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        jdbcTemplate: NamedParameterJdbcTemplate,
    ): ItemReader<ProductRankAggregation> {
        val date = LocalDate.parse(requestDate)
        val periodType = RankingPeriodType.WEEKLY
        return RankingAggregationReader(jdbcTemplate, periodType.periodStartDate(date), periodType.periodEndDate(date))
    }

    @StepScope
    @Bean("weeklyProcessor")
    fun weeklyProcessor(): ItemProcessor<ProductRankAggregation, ProductRankResult> = RankingAggregationProcessor()

    @StepScope
    @Bean("weeklyWriter")
    fun weeklyWriter(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        weeklyRepository: ProductRankWeeklyRepository,
        monthlyRepository: ProductRankMonthlyRepository,
    ): ItemWriter<ProductRankResult> =
        RankingAggregationWriter(RankingPeriodType.WEEKLY, LocalDate.parse(requestDate), weeklyRepository, monthlyRepository)

    // --- @StepScope Beans (Monthly) ---

    @StepScope
    @Bean("monthlyCleanupTasklet")
    fun monthlyCleanupTasklet(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        weeklyRepository: ProductRankWeeklyRepository,
        monthlyRepository: ProductRankMonthlyRepository,
    ): Tasklet =
        RankingCleanupTasklet(RankingPeriodType.MONTHLY, LocalDate.parse(requestDate), weeklyRepository, monthlyRepository)

    @StepScope
    @Bean("monthlyReader")
    fun monthlyReader(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        jdbcTemplate: NamedParameterJdbcTemplate,
    ): ItemReader<ProductRankAggregation> {
        val date = LocalDate.parse(requestDate)
        val periodType = RankingPeriodType.MONTHLY
        return RankingAggregationReader(jdbcTemplate, periodType.periodStartDate(date), periodType.periodEndDate(date))
    }

    @StepScope
    @Bean("monthlyProcessor")
    fun monthlyProcessor(): ItemProcessor<ProductRankAggregation, ProductRankResult> = RankingAggregationProcessor()

    @StepScope
    @Bean("monthlyWriter")
    fun monthlyWriter(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        weeklyRepository: ProductRankWeeklyRepository,
        monthlyRepository: ProductRankMonthlyRepository,
    ): ItemWriter<ProductRankResult> =
        RankingAggregationWriter(RankingPeriodType.MONTHLY, LocalDate.parse(requestDate), weeklyRepository, monthlyRepository)
}
