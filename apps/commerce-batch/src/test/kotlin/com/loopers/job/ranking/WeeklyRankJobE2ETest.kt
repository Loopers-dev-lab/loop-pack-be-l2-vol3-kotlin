package com.loopers.job.ranking

import com.loopers.batch.job.ranking.weekly.WeeklyRankJobConfig
import com.loopers.infrastructure.ranking.WeeklyProductRankJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${WeeklyRankJobConfig.JOB_NAME}"])
@Sql(scripts = ["classpath:schema-product-metrics-daily.sql"])
class WeeklyRankJobE2ETest
    @Autowired
    constructor(
        // IDE 정적 분석에서 주입 오류처럼 보일 수 있음 — @SpringBatchTest가 Scope 기반으로 주입하므로 정상.
        private val jobLauncherTestUtils: JobLauncherTestUtils,
        @param:Qualifier(WeeklyRankJobConfig.JOB_NAME) private val job: Job,
        private val weeklyProductRankJpaRepository: WeeklyProductRankJpaRepository,
        private val jdbcTemplate: JdbcTemplate,
    ) {
        @AfterEach
        fun tearDown() {
            weeklyProductRankJpaRepository.deleteAll()
            jdbcTemplate.execute("DELETE FROM product_metrics_daily")
        }

        @Nested
        @DisplayName("주간 랭킹 집계 (WeeklyRankJob)")
        inner class WeeklyAggregation {
            @Test
            @DisplayName("7일치 daily 데이터를 집계해 weekly MV에 TOP 100을 적재하고 rank 번호를 부여한다")
            fun aggregate_weeklyTop100() {
                // arrange: 2026-04-10 ~ 2026-04-16 (7일치)
                val baseDate = LocalDate.of(2026, 4, 16)
                for (dayOffset in 0L..6L) {
                    val date = baseDate.minusDays(dayOffset)
                    insertDaily(productId = 100L, date = date, view = 10, like = 2, units = 1, amount = 10_000L, orderScore = 6.45)
                    insertDaily(productId = 101L, date = date, view = 5, like = 1, units = 0, amount = 0L, orderScore = 0.0)
                }

                // act
                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                // assert
                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val results = weeklyProductRankJpaRepository.findAll()
                // 7일치 누적 기댓값: view 10×7=70, like 2×7=14, units 1×7=7, amount 10_000×7=70_000
                assertAll(
                    { assertThat(results).hasSize(2) },
                    { assertThat(results.first { it.productId == 100L }.rankNumber).isEqualTo(1) },
                    { assertThat(results.first { it.productId == 101L }.rankNumber).isEqualTo(2) },
                    { assertThat(results.first { it.productId == 100L }.viewCount).isEqualTo(70) },
                    { assertThat(results.first { it.productId == 100L }.likeCount).isEqualTo(14) },
                    { assertThat(results.first { it.productId == 100L }.unitsSold).isEqualTo(7) },
                    { assertThat(results.first { it.productId == 100L }.salesAmount).isEqualTo(70_000L) },
                )
            }

            @Test
            @DisplayName("101개 상품 데이터 중 TOP 100만 MV에 적재된다 (LIMIT 경계)")
            fun aggregate_limitTop100() {
                val baseDate = LocalDate.of(2026, 4, 16)
                // productId 1..101. productId가 높을수록 view_count 높음 → totalScore 높음.
                // view_count = productId × 10 (고유값으로 동점 방지). totalScore = viewCount × 0.1 = productId.
                for (productId in 1L..101L) {
                    insertDaily(
                        productId = productId,
                        date = baseDate,
                        view = (productId * 10).toInt(),
                        like = 0,
                        units = 0,
                        amount = 0L,
                        orderScore = 0.0,
                    )
                }

                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val results = weeklyProductRankJpaRepository.findAll()
                assertAll(
                    { assertThat(results).hasSize(100) },
                    // productId 101 (totalScore 최고)이 rank 1
                    { assertThat(results.first { it.rankNumber == 1 }.productId).isEqualTo(101L) },
                    // 최하위 rank 100은 productId 2 (1은 LIMIT에서 잘림)
                    { assertThat(results.first { it.rankNumber == 100 }.productId).isEqualTo(2L) },
                    // productId 1은 MV에 없어야 함 (cutoff)
                    { assertThat(results.none { it.productId == 1L }).isTrue() },
                )
            }

            @Test
            @DisplayName("totalScore 동점 상품은 product_id ASC 로 tie-break하여 rank가 결정된다")
            fun aggregate_tieBreakByProductIdAsc() {
                val baseDate = LocalDate.of(2026, 4, 16)
                // 두 상품 동일 view_count → totalScore 동점. product_id ASC로 안정 정렬.
                insertDaily(productId = 200L, date = baseDate, view = 50, like = 0, units = 0, amount = 0L, orderScore = 0.0)
                insertDaily(productId = 100L, date = baseDate, view = 50, like = 0, units = 0, amount = 0L, orderScore = 0.0)

                jobLauncherTestUtils.job = job
                jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                val results = weeklyProductRankJpaRepository.findAll()
                // 동점 시 product_id 낮은 쪽이 rank 1
                assertAll(
                    { assertThat(results.first { it.productId == 100L }.rankNumber).isEqualTo(1) },
                    { assertThat(results.first { it.productId == 200L }.rankNumber).isEqualTo(2) },
                )
            }

            @Test
            @DisplayName("같은 (year, week)로 재실행하면 기존 MV 데이터가 2회차 입력 기준으로 교체된다 (멱등성)")
            fun aggregate_idempotent() {
                val baseDate = LocalDate.of(2026, 4, 16)

                // 1회차: viewCount 10
                insertDaily(productId = 100L, date = baseDate, view = 10, like = 2, units = 1, amount = 10_000L, orderScore = 6.45)
                jobLauncherTestUtils.job = job
                val params1 = JobParametersBuilder()
                    .addString("baseDate", "2026-04-16")
                    .addLong("run.id", 1L)
                    .toJobParameters()
                jobLauncherTestUtils.launchJob(params1)

                // 1회차 결과 검증
                val afterFirst = weeklyProductRankJpaRepository.findAll()
                assertThat(afterFirst).hasSize(1)
                assertThat(afterFirst.first().viewCount).isEqualTo(10)

                // 입력 데이터 교체 (2회차는 viewCount 50 기준이 되어야 함)
                jdbcTemplate.execute("DELETE FROM product_metrics_daily")
                insertDaily(productId = 100L, date = baseDate, view = 50, like = 5, units = 3, amount = 30_000L, orderScore = 15.0)

                val params2 = JobParametersBuilder()
                    .addString("baseDate", "2026-04-16")
                    .addLong("run.id", 2L)
                    .toJobParameters()
                val jobExecution = jobLauncherTestUtils.launchJob(params2)

                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val afterSecond = weeklyProductRankJpaRepository.findAll()
                // 멱등 교체: 기존 행 삭제 후 2회차 입력 기준으로 재적재
                assertAll(
                    { assertThat(afterSecond).hasSize(1) },
                    { assertThat(afterSecond.first().productId).isEqualTo(100L) },
                    { assertThat(afterSecond.first().viewCount).isEqualTo(50) },
                    { assertThat(afterSecond.first().likeCount).isEqualTo(5) },
                    { assertThat(afterSecond.first().unitsSold).isEqualTo(3) },
                    { assertThat(afterSecond.first().salesAmount).isEqualTo(30_000L) },
                )
            }

            @Test
            @DisplayName("baseDate - 7일과 baseDate + 1일의 행은 집계에서 제외된다 (윈도우 경계)")
            fun aggregate_windowBoundary() {
                val baseDate = LocalDate.of(2026, 4, 16)

                // 윈도우 안쪽: baseDate - 6 ~ baseDate (7일)
                for (dayOffset in 0L..6L) {
                    insertDaily(
                        productId = 100L,
                        date = baseDate.minusDays(dayOffset),
                        view = 10,
                        like = 0,
                        units = 0,
                        amount = 0L,
                        orderScore = 0.0,
                    )
                }
                // 윈도우 밖: baseDate - 7 (이전 주)
                insertDaily(productId = 100L, date = baseDate.minusDays(7), view = 999, like = 999, units = 999, amount = 999L, orderScore = 999.0)
                // 윈도우 밖: baseDate + 1 (다음 날)
                insertDaily(productId = 100L, date = baseDate.plusDays(1), view = 999, like = 999, units = 999, amount = 999L, orderScore = 999.0)

                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val results = weeklyProductRankJpaRepository.findAll()
                // 윈도우 안쪽 7일치만 집계: view 10*7 = 70 (999*2 = 1998 더해지면 안 됨)
                assertAll(
                    { assertThat(results).hasSize(1) },
                    { assertThat(results.first().productId).isEqualTo(100L) },
                    { assertThat(results.first().viewCount).isEqualTo(70) },
                    { assertThat(results.first().likeCount).isEqualTo(0) },
                )
            }

            @Test
            @DisplayName("deleted_at이 설정된 daily 행은 SUM 집계에서 제외된다 (soft-delete 필터)")
            fun aggregate_softDeleteFilter() {
                val baseDate = LocalDate.of(2026, 4, 16)

                // 정상 행 (deleted_at NULL)
                insertDaily(productId = 100L, date = baseDate, view = 10, like = 2, units = 1, amount = 10_000L, orderScore = 6.45)
                // soft-delete 처리된 행 (같은 날, 같은 상품)
                insertDailyWithDeleted(
                    productId = 100L,
                    date = baseDate.minusDays(1),
                    view = 999,
                    like = 999,
                    units = 999,
                    amount = 999L,
                    orderScore = 999.0,
                )

                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val results = weeklyProductRankJpaRepository.findAll()
                // soft-delete 행은 SUM에서 제외
                assertAll(
                    { assertThat(results).hasSize(1) },
                    { assertThat(results.first().productId).isEqualTo(100L) },
                    { assertThat(results.first().viewCount).isEqualTo(10) },
                    { assertThat(results.first().likeCount).isEqualTo(2) },
                    { assertThat(results.first().unitsSold).isEqualTo(1) },
                    { assertThat(results.first().salesAmount).isEqualTo(10_000L) },
                )
            }
        }

        /**
         * 여러 테스트가 같은 baseDate를 쓰면 Spring Batch가 JobInstance를 공유하여
         * `JobInstanceAlreadyCompleteException`이 발생한다.
         * 테스트별 고유 `run.id`를 identifying parameter로 추가해 신규 JobInstance를 생성한다.
         */
        private fun uniqueParams(baseDateStr: String): org.springframework.batch.core.JobParameters =
            JobParametersBuilder()
                .addString("baseDate", baseDateStr)
                .addLong("run.id", System.nanoTime())
                .toJobParameters()

        private fun insertDaily(
            productId: Long,
            date: LocalDate,
            view: Int,
            like: Int,
            units: Int,
            amount: Long,
            orderScore: Double,
        ) {
            jdbcTemplate.update(
                """
                INSERT INTO product_metrics_daily
                    (product_id, metric_date, view_count, like_count, units_sold, sales_amount, order_score, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6))
                """.trimIndent(),
                productId,
                date,
                view,
                like,
                units,
                amount,
                orderScore,
            )
        }

        private fun insertDailyWithDeleted(
            productId: Long,
            date: LocalDate,
            view: Int,
            like: Int,
            units: Int,
            amount: Long,
            orderScore: Double,
        ) {
            jdbcTemplate.update(
                """
                INSERT INTO product_metrics_daily
                    (product_id, metric_date, view_count, like_count, units_sold, sales_amount, order_score, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6), NOW(6))
                """.trimIndent(),
                productId,
                date,
                view,
                like,
                units,
                amount,
                orderScore,
            )
        }
    }
