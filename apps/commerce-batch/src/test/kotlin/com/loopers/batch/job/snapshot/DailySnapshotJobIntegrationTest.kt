package com.loopers.batch.job.snapshot

import com.loopers.domain.ranking.ProductMetricsDailyRepository
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
class DailySnapshotJobIntegrationTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(DailySnapshotJobConfig.JOB_NAME) private val job: Job,
    private val redisZSetTemplate: RedisZSetTemplate,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils.job = job
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @DisplayName("DailySnapshotJob 실행")
    @Test
    fun `Redis ZSET의 전체 랭킹을 rank_position까지 product_metrics_daily에 덤프한다`() {
        val date = LocalDate.of(2026, 1, 1)
        seedRanking(date, listOf("10" to 300.0, "20" to 200.0, "30" to 100.0))

        val execution = jobLauncherTestUtils.launchJob(jobParams(date))

        val dumped = productMetricsDailyRepository.findAllDailyOn(date).sortedBy { it.rankPosition }
        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(dumped).hasSize(3) },
            { assertThat(dumped.map { it.productId }).containsExactly(10L, 20L, 30L) },
            { assertThat(dumped.map { it.rankPosition }).containsExactly(1, 2, 3) },
            { assertThat(dumped.map { it.totalScore }).containsExactly(300.0, 200.0, 100.0) },
            { assertThat(dumped).allMatch { it.viewCount == 0L && it.likeCount == 0L && it.orderCount == 0L } },
        )
    }

    @Test
    fun `같은 날짜로 두 번 실행하면 중복 행 없이 최신 점수로 upsert된다`() {
        val date = LocalDate.of(2026, 1, 2)
        seedRanking(date, listOf("1" to 10.0, "2" to 20.0))
        jobLauncherTestUtils.launchJob(jobParams(date))

        redisZSetTemplate.delete(RankingKeyGenerator.dailyKey(date))
        seedRanking(date, listOf("1" to 99.9, "2" to 55.5, "3" to 11.1))
        val second = jobLauncherTestUtils.launchJob(jobParams(date))

        val dumped = productMetricsDailyRepository.findAllDailyOn(date).sortedBy { it.productId }
        assertAll(
            { assertThat(second.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(dumped).hasSize(3) },
            { assertThat(dumped.first { it.productId == 1L }.totalScore).isEqualTo(99.9) },
            { assertThat(dumped.first { it.productId == 2L }.totalScore).isEqualTo(55.5) },
            { assertThat(dumped.first { it.productId == 3L }.totalScore).isEqualTo(11.1) },
        )
    }

    @Test
    fun `대상 ZSET이 비어 있어도 에러 없이 0건 처리로 완료된다`() {
        val date = LocalDate.of(2026, 1, 3)

        val execution = jobLauncherTestUtils.launchJob(jobParams(date))

        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(productMetricsDailyRepository.countDailyOn(date)).isZero() },
        )
    }

    @Test
    fun `member가 숫자가 아닌 레코드는 해당 건만 skip하고 나머지는 정상 적재된다`() {
        val date = LocalDate.of(2026, 1, 4)
        seedRanking(date, listOf("100" to 50.0, "not-a-number" to 40.0, "200" to 30.0))

        val execution = jobLauncherTestUtils.launchJob(jobParams(date))
        val stepSkipCount = execution.stepExecutions.sumOf { it.skipCount }
        val dumped = productMetricsDailyRepository.findAllDailyOn(date)

        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(stepSkipCount).isEqualTo(1L) },
            { assertThat(dumped.map { it.productId }).containsExactlyInAnyOrder(100L, 200L) },
        )
    }

    private fun seedRanking(date: LocalDate, entries: List<Pair<String, Double>>) {
        val key = RankingKeyGenerator.dailyKey(date)
        entries.forEach { (member, score) -> redisZSetTemplate.incrementScore(key, member, score) }
    }

    private fun jobParams(date: LocalDate) =
        JobParametersBuilder()
            .addLocalDate(DailySnapshotJobListener.PARAM_TARGET_DATE, date)
            .addLong("trigger", TRIGGER_SEQ.incrementAndGet())
            .toJobParameters()

    companion object {
        private val TRIGGER_SEQ = AtomicLong(0)
    }
}
