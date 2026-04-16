package com.loopers.job.ranking

import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.slf4j.LoggerFactory
import org.springframework.test.context.TestPropertySource
import java.io.File
import java.time.LocalDate

/**
 * TC-1: Chunk vs Tasklet 속도 벤치마크.
 *
 * 동일 데이터에서 두 방식의 wall-time을 측정하여 트레이드오프를 실측한다.
 * 각 케이스: 데이터 시딩 → Job 실행 → 시간 측정 → 결과 검증 → 데이터 정리
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "spring.batch.job.enabled=false",
        "spring.batch.job.name=weeklyRankingJob",
    ],
)
class RankingJobBenchmarkTest {

    companion object {
        private val log = LoggerFactory.getLogger(RankingJobBenchmarkTest::class.java)
        private val resultFile = File("build/bench-chunk-results.txt")
    }

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var databaseCleanUp: DatabaseCleanUp

    @Autowired
    @Qualifier("weeklyRankingJob")
    private lateinit var chunkJob: Job

    private val startDate = LocalDate.of(2026, 4, 6)
    private val endDate = LocalDate.of(2026, 4, 12)
    private val periodKey = "2026-W15"

    @BeforeEach
    fun setUp() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("TC-1: Chunk vs Tasklet 속도 비교")
    inner class SpeedBenchmark {

        @Test
        @DisplayName("Chunk Job — 1k/5k/10k/50k/100k 상품 벤치마크")
        fun chunkJobBenchmark() {
            val seeds = listOf(1_000, 5_000, 10_000, 50_000, 100_000)
            val results = mutableListOf<BenchResult>()

            for (productCount in seeds) {
                databaseCleanUp.truncateAllTables()

                val rows = ProductMetricsSeeder.seed(jdbcTemplate, productCount, startDate, 7)

                val params = JobParametersBuilder()
                    .addString("periodType", "WEEKLY")
                    .addString("periodKey", periodKey)
                    .addString("startDate", startDate.toString())
                    .addString("endDate", endDate.toString())
                    .addLong("run", System.nanoTime())
                    .toJobParameters()

                val start = System.nanoTime()
                val execution = jobLauncher.run(chunkJob, params)
                val elapsed = (System.nanoTime() - start) / 1_000_000

                assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)

                val mvCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ?",
                    Long::class.java,
                    periodKey,
                ) ?: 0L
                val expectedCount = minOf(productCount, 100).toLong()
                assertThat(mvCount).isEqualTo(expectedCount)

                results.add(BenchResult("Chunk", productCount, rows, elapsed))
                log.warn("[BENCH] job=weeklyRankingJob  seed=$productCount  rows=$rows  elapsed=${elapsed}ms  exit=${execution.status}")
            }

            resultFile.parentFile.mkdirs()
            resultFile.writeText(results.joinToString("\n") { it.toString() })
            log.warn("\n========== Chunk 벤치마크 결과 ==========")
            results.forEach { log.warn(it.toString()) }
        }
    }

    data class BenchResult(
        val jobType: String,
        val productCount: Int,
        val rows: Int,
        val elapsedMs: Long,
    ) {
        override fun toString(): String =
            "[BENCH] $jobType  products=$productCount  rows=$rows  elapsed=${elapsedMs}ms"
    }
}
