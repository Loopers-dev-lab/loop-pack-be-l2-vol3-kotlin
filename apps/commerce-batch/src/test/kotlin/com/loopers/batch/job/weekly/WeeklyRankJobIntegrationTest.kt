package com.loopers.batch.job.weekly

import com.loopers.domain.ranking.ProductMetricsDaily
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import com.loopers.domain.ranking.WeeklyRankRepository
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
class WeeklyRankJobIntegrationTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(WeeklyRankJobConfig.JOB_NAME) private val job: Job,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val weeklyRankRepository: WeeklyRankRepository,
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
    fun `7일치 daily를 합산해 Top 순위로 mv_product_rank_weekly에 적재한다`() {
        val weekEnd = LocalDate.of(2026, 2, 7)
        (0 until 7).forEach { offset ->
            val date = weekEnd.minusDays(offset.toLong())
            seedDaily(productId = 1L, date = date, totalScore = 10.0)
            seedDaily(productId = 2L, date = date, totalScore = 5.0)
            seedDaily(productId = 3L, date = date, totalScore = 1.0)
        }

        val execution = jobLauncherTestUtils.launchJob(jobParams(weekEnd))

        val ranks = weeklyRankRepository.findRanksByWeekEnd(weekEnd)
        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(ranks).hasSize(3) },
            { assertThat(ranks.map { it.productId }).containsExactly(1L, 2L, 3L) },
            { assertThat(ranks.map { it.rankPosition }).containsExactly(1, 2, 3) },
            { assertThat(ranks.map { it.totalScore }).containsExactly(70.0, 35.0, 7.0) },
            { assertThat(ranks).allMatch { it.weekStart == weekEnd.minusDays(6) && it.weekEnd == weekEnd } },
        )
    }

    @Test
    fun `점수 동률이어도 ROW_NUMBER로 서로 다른 순위가 부여된다`() {
        val weekEnd = LocalDate.of(2026, 2, 14)
        (0 until 7).forEach { offset ->
            val date = weekEnd.minusDays(offset.toLong())
            seedDaily(productId = 10L, date = date, totalScore = 10.0)
            seedDaily(productId = 20L, date = date, totalScore = 10.0)
        }

        jobLauncherTestUtils.launchJob(jobParams(weekEnd))

        val ranks = weeklyRankRepository.findRanksByWeekEnd(weekEnd)
        assertAll(
            { assertThat(ranks).hasSize(2) },
            { assertThat(ranks.map { it.rankPosition }).containsExactlyInAnyOrder(1, 2) },
            { assertThat(ranks).allMatch { it.totalScore == 70.0 } },
            { assertThat(ranks.map { it.productId }).containsExactlyInAnyOrder(10L, 20L) },
        )
    }

    @Test
    fun `7일 중 하루 누락되어도 나머지 6일치로 집계한다`() {
        val weekEnd = LocalDate.of(2026, 2, 21)
        val missing = weekEnd.minusDays(3)
        (0 until 7).forEach { offset ->
            val date = weekEnd.minusDays(offset.toLong())
            if (date != missing) {
                seedDaily(productId = 1L, date = date, totalScore = 10.0)
            }
        }

        jobLauncherTestUtils.launchJob(jobParams(weekEnd))

        val ranks = weeklyRankRepository.findRanksByWeekEnd(weekEnd)
        assertAll(
            { assertThat(ranks).hasSize(1) },
            { assertThat(ranks.first().totalScore).isEqualTo(60.0) },
            { assertThat(ranks.first().rankPosition).isEqualTo(1) },
        )
    }

    @Test
    fun `같은 week_end로 재실행하면 중복 없이 최신 점수로 upsert된다`() {
        val weekEnd = LocalDate.of(2026, 2, 28)
        (0 until 7).forEach { offset ->
            seedDaily(productId = 1L, date = weekEnd.minusDays(offset.toLong()), totalScore = 5.0)
        }
        jobLauncherTestUtils.launchJob(jobParams(weekEnd))

        databaseCleanUp.truncateAllTables()
        (0 until 7).forEach { offset ->
            seedDaily(productId = 1L, date = weekEnd.minusDays(offset.toLong()), totalScore = 20.0)
        }
        val second = jobLauncherTestUtils.launchJob(jobParams(weekEnd))

        val ranks = weeklyRankRepository.findRanksByWeekEnd(weekEnd)
        assertAll(
            { assertThat(second.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(ranks).hasSize(1) },
            { assertThat(ranks.first().totalScore).isEqualTo(140.0) },
        )
    }

    @Test
    fun `다른 week_end로 두 번 실행하면 두 버전이 모두 보존되고 findLatestWeekEnd는 최신값을 반환한다`() {
        val firstWeekEnd = LocalDate.of(2026, 3, 7)
        val secondWeekEnd = LocalDate.of(2026, 3, 14)
        (0 until 7).forEach { offset ->
            seedDaily(productId = 1L, date = firstWeekEnd.minusDays(offset.toLong()), totalScore = 10.0)
        }
        jobLauncherTestUtils.launchJob(jobParams(firstWeekEnd))

        (0 until 7).forEach { offset ->
            seedDaily(productId = 1L, date = secondWeekEnd.minusDays(offset.toLong()), totalScore = 5.0)
        }
        jobLauncherTestUtils.launchJob(jobParams(secondWeekEnd))

        assertAll(
            { assertThat(weeklyRankRepository.findLatestWeekEnd()).isEqualTo(secondWeekEnd) },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(firstWeekEnd)).hasSize(1) },
            { assertThat(weeklyRankRepository.findRanksByWeekEnd(secondWeekEnd)).hasSize(1) },
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
            .addLocalDate(WeeklyRankJobListener.PARAM_TARGET_DATE, targetDate)
            .addLong("trigger", TRIGGER_SEQ.incrementAndGet())
            .toJobParameters()

    companion object {
        private val TRIGGER_SEQ = AtomicLong(0)
    }
}
