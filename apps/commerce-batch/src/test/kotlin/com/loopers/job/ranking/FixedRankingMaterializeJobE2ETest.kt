package com.loopers.job.ranking

import com.loopers.CommerceBatchApplication
import com.loopers.batch.job.ranking.FixedRankingJobConfig
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.infrastructure.ranking.ProductRankMonthlyMvJpaRepository
import com.loopers.infrastructure.ranking.ProductRankWeeklyMvJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@SpringBootTest(classes = [CommerceBatchApplication::class])
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${FixedRankingJobConfig.JOB_NAME}"])
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
class FixedRankingMaterializeJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(FixedRankingJobConfig.JOB_NAME) private val job: Job,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val productRankWeeklyMvJpaRepository: ProductRankWeeklyMvJpaRepository,
    private val productRankMonthlyMvJpaRepository: ProductRankMonthlyMvJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `week-fixed scope로 실행하면 해당 주간 TOP 100 MV를 저장한다`() {
        seedMetric(1L, LocalDate.of(2026, 4, 14), 5.0)
        seedMetric(1L, LocalDate.of(2026, 4, 16), 7.0)
        seedMetric(2L, LocalDate.of(2026, 4, 15), 9.0)
        seedMetric(3L, LocalDate.of(2026, 4, 18), 1.0)

        jobLauncherTestUtils.job = job
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("scope", "week-fixed")
                .addString("targetDate", "20260417")
                .addLong("run.id", System.nanoTime())
                .toJobParameters(),
        )

        assertThat(execution.exitStatus.exitCode).isEqualTo("COMPLETED")

        val snapshots = productRankWeeklyMvJpaRepository.findAllByPeriodStartDate(
            LocalDate.of(2026, 4, 13),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "rankPosition")),
        )

        assertThat(snapshots.totalElements).isEqualTo(3)
        assertThat(snapshots.content.map { it.productId }).containsExactly(1L, 2L, 3L)
        assertThat(snapshots.content.map { it.score }).containsExactly(12.0, 9.0, 1.0)
        assertThat(snapshots.content.first().periodEndDate).isEqualTo(LocalDate.of(2026, 4, 19))
    }

    @Test
    fun `month-fixed scope로 실행하면 해당 월간 TOP 100 MV를 저장한다`() {
        seedMetric(10L, LocalDate.of(2026, 4, 1), 1.0)
        seedMetric(10L, LocalDate.of(2026, 4, 20), 3.0)
        seedMetric(11L, LocalDate.of(2026, 4, 10), 10.0)
        seedMetric(12L, LocalDate.of(2026, 3, 31), 99.0)

        jobLauncherTestUtils.job = job
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("scope", "month-fixed")
                .addString("targetDate", "20260417")
                .addLong("run.id", System.nanoTime())
                .toJobParameters(),
        )

        assertThat(execution.exitStatus.exitCode).isEqualTo("COMPLETED")

        val snapshots = productRankMonthlyMvJpaRepository.findAllByPeriodStartDate(
            LocalDate.of(2026, 4, 1),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "rankPosition")),
        )

        assertThat(snapshots.totalElements).isEqualTo(2)
        assertThat(snapshots.content.map { it.productId }).containsExactly(11L, 10L)
        assertThat(snapshots.content.map { it.score }).containsExactly(10.0, 4.0)
        assertThat(snapshots.content.first().periodEndDate).isEqualTo(LocalDate.of(2026, 4, 30))
    }

    private fun seedMetric(productId: Long, metricsDate: LocalDate, rankingScore: Double) {
        productMetricsJpaRepository.save(
            ProductMetricsModel(
                productId = productId,
                metricsDate = metricsDate,
                rankingScore = rankingScore,
            ),
        )
    }
}
