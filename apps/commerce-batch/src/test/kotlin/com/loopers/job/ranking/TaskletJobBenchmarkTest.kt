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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.slf4j.LoggerFactory
import org.springframework.test.context.TestPropertySource
import java.io.File
import java.time.LocalDate

/**
 * TC-1: Tasklet Job 속도 벤치마크.
 *
 * Chunk 벤치마크와 동일한 데이터셋에서 Tasklet 방식의 wall-time을 측정한다.
 * RankingJobBenchmarkTest와 결과를 비교하여 트레이드오프를 실측한다.
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "spring.batch.job.enabled=false",
        "spring.batch.job.name=weeklyRankingTaskletJob",
    ],
)
class TaskletJobBenchmarkTest {

    companion object {
        private val log = LoggerFactory.getLogger(TaskletJobBenchmarkTest::class.java)
        private val resultFile = File("build/bench-tasklet-results.txt")
    }

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var databaseCleanUp: DatabaseCleanUp

    @Autowired
    @Qualifier("weeklyRankingTaskletJob")
    private lateinit var taskletJob: Job

    private val startDate = LocalDate.of(2026, 4, 6)
    private val endDate = LocalDate.of(2026, 4, 12)
    private val periodKey = "2026-W15"

    @BeforeEach
    fun setUp() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("TC-1: Tasklet 속도 벤치마크")
    inner class SpeedBenchmark {

        @Test
        @DisplayName("Tasklet Job — 1k/5k/10k/50k/100k 상품 벤치마크")
        fun taskletJobBenchmark() {
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
                val execution = jobLauncher.run(taskletJob, params)
                val elapsed = (System.nanoTime() - start) / 1_000_000

                assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)

                val mvCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ?",
                    Long::class.java,
                    periodKey,
                ) ?: 0L
                val expectedCount = minOf(productCount, 100).toLong()
                assertThat(mvCount).isEqualTo(expectedCount)

                results.add(BenchResult("Tasklet", productCount, rows, elapsed))
                log.warn("[BENCH] job=weeklyRankingTaskletJob  seed=$productCount  rows=$rows  elapsed=${elapsed}ms  exit=${execution.status}")
            }

            resultFile.parentFile.mkdirs()
            resultFile.writeText(results.joinToString("\n") { it.toString() })
            log.warn("\n========== Tasklet 벤치마크 결과 ==========")
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
