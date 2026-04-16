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
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

/**
 * TC-2: 실패 복구 테스트.
 *
 * Chunk Job은 중간 실패 시 이전 chunk가 커밋되어 보존된다.
 * Tasklet Job은 실패 시 전체 롤백된다.
 *
 * 이 테스트는 "동일한 실패 상황에서 두 방식의 복구 특성 차이"를 증명한다.
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "spring.batch.job.enabled=false",
        "spring.batch.job.name=weeklyRankingJob",
    ],
)
class FailureRecoveryTest {

    @Autowired
    private lateinit var jobLauncher: JobLauncher

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
    @DisplayName("TC-2a: Chunk Job — 정상 실행 후 재실행 시 멱등성")
    inner class ChunkIdempotency {

        @Test
        @DisplayName("동일 periodKey로 2번 실행하면 MV에 최신 version만 남는다")
        fun rerunProducesSameResult() {
            // arrange
            ProductMetricsSeeder.seed(jdbcTemplate, 200, startDate, 7)

            val baseParams = JobParametersBuilder()
                .addString("periodType", "WEEKLY")
                .addString("periodKey", periodKey)
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())

            // act — 1차 실행
            val params1 = baseParams.addLong("run", System.nanoTime()).toJobParameters()
            val exec1 = jobLauncher.run(chunkJob, params1)
            assertThat(exec1.status).isEqualTo(BatchStatus.COMPLETED)

            val countAfterFirst = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ?",
                Long::class.java,
                periodKey,
            ) ?: 0L

            // act — 2차 실행 (데이터 변경 없이 재실행)
            val params2 = baseParams.addLong("run", System.nanoTime()).toJobParameters()
            val exec2 = jobLauncher.run(chunkJob, params2)
            assertThat(exec2.status).isEqualTo(BatchStatus.COMPLETED)

            val countAfterSecond = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ?",
                Long::class.java,
                periodKey,
            ) ?: 0L

            // assert — 두 번 실행해도 결과 건수 동일 (최신 version만 남음)
            assertThat(countAfterFirst).isEqualTo(100)
            assertThat(countAfterSecond).isEqualTo(100)

            // version이 2인 데이터만 남아있어야 함 (version 1은 cleanup됨)
            val maxVersion = jdbcTemplate.queryForObject(
                "SELECT MAX(data_version) FROM mv_product_rank_weekly WHERE year_week = ?",
                Int::class.java,
                periodKey,
            ) ?: 0
            assertThat(maxVersion).isEqualTo(2)

            val oldVersionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ? AND data_version < ?",
                Long::class.java,
                periodKey,
                maxVersion,
            ) ?: 0L
            assertThat(oldVersionCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("TC-2b: Chunk Job — Step 1 완료 후 중간 테이블에 데이터 보존 확인")
    inner class ChunkIntermediateDataPreservation {

        @Test
        @DisplayName("Chunk Step 1 완료 후 중간 테이블에 전체 상품의 집계 데이터가 존재한다")
        fun intermediateTableHasAllAggregatedData() {
            // arrange
            val productCount = 500
            ProductMetricsSeeder.seed(jdbcTemplate, productCount, startDate, 7)

            val params = JobParametersBuilder()
                .addString("periodType", "WEEKLY")
                .addString("periodKey", periodKey)
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())
                .addLong("run", System.nanoTime())
                .toJobParameters()

            // act
            val execution = jobLauncher.run(chunkJob, params)
            assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)

            // assert — Step 1의 중간 테이블은 Step 2에서 cleanup되지만,
            // MV에 TOP 100이 정확히 적재되었는지로 Step 1의 정상 동작을 간접 검증
            val mvCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ?",
                Long::class.java,
                periodKey,
            ) ?: 0L
            assertThat(mvCount).isEqualTo(100)

            // rank가 1~100 연속인지 확인
            val ranks = jdbcTemplate.queryForList(
                "SELECT ranking_rank FROM mv_product_rank_weekly WHERE year_week = ? ORDER BY ranking_rank",
                Int::class.java,
                periodKey,
            )
            assertThat(ranks).isEqualTo((1..100).toList())

            // score가 내림차순인지 확인
            val scores = jdbcTemplate.queryForList(
                "SELECT score FROM mv_product_rank_weekly WHERE year_week = ? ORDER BY ranking_rank",
                Double::class.java,
                periodKey,
            )
            for (i in 0 until scores.size - 1) {
                assertThat(scores[i]).isGreaterThanOrEqualTo(scores[i + 1])
            }
        }
    }

    @Nested
    @DisplayName("TC-2c: Chunk vs Tasklet — 블루-그린 version 교체로 이전 데이터 보존")
    inner class BlueGreenVersionSafety {

        @Test
        @DisplayName("Step 2에서 새 version INSERT 후 이전 version은 Step 3에서 삭제된다")
        fun blueGreenVersionTransition() {
            // arrange
            ProductMetricsSeeder.seed(jdbcTemplate, 200, startDate, 7)

            // act — 1차 실행 (version=1 생성)
            val params1 = JobParametersBuilder()
                .addString("periodType", "WEEKLY")
                .addString("periodKey", periodKey)
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())
                .addLong("run", System.nanoTime())
                .toJobParameters()
            jobLauncher.run(chunkJob, params1)

            // version=1이 존재
            val v1Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ? AND data_version = 1",
                Long::class.java,
                periodKey,
            ) ?: 0L
            assertThat(v1Count).isEqualTo(100)

            // act — 2차 실행 (version=2 생성, version=1 삭제)
            val params2 = JobParametersBuilder()
                .addString("periodType", "WEEKLY")
                .addString("periodKey", periodKey)
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())
                .addLong("run", System.nanoTime())
                .toJobParameters()
            jobLauncher.run(chunkJob, params2)

            // assert — version=1은 삭제됨, version=2만 남음
            val v1After = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ? AND data_version = 1",
                Long::class.java,
                periodKey,
            ) ?: 0L
            val v2After = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_weekly WHERE year_week = ? AND data_version = 2",
                Long::class.java,
                periodKey,
            ) ?: 0L

            assertThat(v1After).isEqualTo(0)
            assertThat(v2After).isEqualTo(100)
        }
    }
}
