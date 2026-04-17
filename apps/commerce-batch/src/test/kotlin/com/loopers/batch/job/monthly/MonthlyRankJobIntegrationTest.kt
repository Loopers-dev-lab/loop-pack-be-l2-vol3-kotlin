package com.loopers.batch.job.monthly

import com.loopers.common.DateUtils
import com.loopers.domain.ranking.MonthlyRankRepository
import com.loopers.domain.ranking.ProductMetricsDaily
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@SpringBatchTest
class MonthlyRankJobIntegrationTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(MonthlyRankJobConfig.JOB_NAME) private val job: Job,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val monthlyRankRepository: MonthlyRankRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils.job = job
        databaseCleanUp.truncateAllTables()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `30일치 daily를 합산해 yyyyMM 버전 키로 mv_product_rank_monthly에 적재한다`() {
        val targetDate = LocalDate.of(2026, 4, 30)
        val yearMonth = DateUtils.formatYearMonth(targetDate)
        listOf(1, 10, 20, 29).forEach { offset ->
            val date = targetDate.minusDays(offset.toLong())
            seedDaily(productId = 1L, date = date, totalScore = 100.0)
            seedDaily(productId = 2L, date = date, totalScore = 50.0)
        }

        val execution = jobLauncherTestUtils.launchJob(jobParams(targetDate))

        val ranks = monthlyRankRepository.findRanksByYearMonth(yearMonth)
        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(ranks).hasSize(2) },
            { assertThat(ranks.map { it.productId }).containsExactly(1L, 2L) },
            { assertThat(ranks.map { it.rankPosition }).containsExactly(1, 2) },
            { assertThat(ranks.map { it.totalScore }).containsExactly(400.0, 200.0) },
            { assertThat(ranks).allMatch { it.yearMonth == yearMonth } },
        )
    }

    @Test
    fun `같은 yearMonth로 재실행하면 중복 없이 upsert된다`() {
        val targetDate = LocalDate.of(2026, 5, 30)
        val yearMonth = DateUtils.formatYearMonth(targetDate)
        seedDaily(productId = 1L, date = targetDate, totalScore = 10.0)
        jobLauncherTestUtils.launchJob(jobParams(targetDate))

        databaseCleanUp.truncateAllTables()
        seedDaily(productId = 1L, date = targetDate, totalScore = 99.0)
        jobLauncherTestUtils.launchJob(jobParams(targetDate))

        val ranks = monthlyRankRepository.findRanksByYearMonth(yearMonth)
        assertAll(
            { assertThat(ranks).hasSize(1) },
            { assertThat(ranks.first().totalScore).isEqualTo(99.0) },
        )
    }

    @Test
    fun `다른 달로 두 번 실행하면 두 버전이 모두 보존되고 findLatestYearMonth는 최신 yearMonth를 반환한다`() {
        val firstDate = LocalDate.of(2026, 6, 30)
        val secondDate = LocalDate.of(2026, 7, 31)
        seedDaily(productId = 1L, date = firstDate, totalScore = 10.0)
        jobLauncherTestUtils.launchJob(jobParams(firstDate))

        seedDaily(productId = 1L, date = secondDate, totalScore = 20.0)
        jobLauncherTestUtils.launchJob(jobParams(secondDate))

        assertAll(
            { assertThat(monthlyRankRepository.findLatestYearMonth()).isEqualTo(DateUtils.formatYearMonth(secondDate)) },
            { assertThat(monthlyRankRepository.findRanksByYearMonth(DateUtils.formatYearMonth(firstDate))).hasSize(1) },
            { assertThat(monthlyRankRepository.findRanksByYearMonth(DateUtils.formatYearMonth(secondDate))).hasSize(1) },
        )
    }

    private fun seedDaily(productId: Long, date: LocalDate, totalScore: Double) {
        productMetricsDailyRepository.save(
            ProductMetricsDaily.create(
                productId = productId,
                metricDate = date,
                totalScore = totalScore,
            ),
        )
    }

    private fun jobParams(targetDate: LocalDate) =
        JobParametersBuilder()
            .addLocalDate(MonthlyRankJobListener.PARAM_TARGET_DATE, targetDate)
            .addLong("trigger", TRIGGER_SEQ.incrementAndGet())
            .toJobParameters()

    companion object {
        private val TRIGGER_SEQ = AtomicLong(0)
    }
}
