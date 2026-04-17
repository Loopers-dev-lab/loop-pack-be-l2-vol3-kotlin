package com.loopers.job.ranking

import com.loopers.batch.job.ranking.monthly.MonthlyRankJobConfig
import com.loopers.infrastructure.ranking.MonthlyProductRankEntity
import com.loopers.infrastructure.ranking.MonthlyProductRankJpaRepository
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
@TestPropertySource(properties = ["spring.batch.job.name=${MonthlyRankJobConfig.JOB_NAME}"])
@Sql(scripts = ["classpath:schema-product-metrics-daily.sql"])
class MonthlyRankJobE2ETest
    @Autowired
    constructor(
        // IDE 정적 분석에서 주입 오류처럼 보일 수 있음 — @SpringBatchTest가 Scope 기반으로 주입하므로 정상.
        private val jobLauncherTestUtils: JobLauncherTestUtils,
        @param:Qualifier(MonthlyRankJobConfig.JOB_NAME) private val job: Job,
        private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
        private val jdbcTemplate: JdbcTemplate,
    ) {
        @AfterEach
        fun tearDown() {
            monthlyProductRankJpaRepository.deleteAll()
            jdbcTemplate.execute("DELETE FROM product_metrics_daily")
        }

        @Nested
        @DisplayName("월간 랭킹 집계 (MonthlyRankJob)")
        inner class MonthlyAggregation {
            @Test
            @DisplayName("한 달치 daily 데이터를 집계해 monthly MV에 TOP N을 적재하고 rank 번호를 부여한다")
            fun aggregate_monthlyTop100() {
                // arrange: 2026-04-01 ~ 2026-04-30 (30일치)
                val baseDate = LocalDate.of(2026, 4, 16)
                for (dayOfMonth in 1..30) {
                    val date = LocalDate.of(2026, 4, dayOfMonth)
                    insertDaily(productId = 100L, date = date, view = 10, like = 2, units = 1, amount = 10_000L, orderScore = 6.45)
                    insertDaily(productId = 101L, date = date, view = 5, like = 1, units = 0, amount = 0L, orderScore = 0.0)
                }

                // act
                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                // assert
                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val results = monthlyProductRankJpaRepository.findAll()
                // 30일치 누적 기댓값: view 10×30=300, like 2×30=60, units 1×30=30, amount 10_000×30=300_000
                assertAll(
                    { assertThat(results).hasSize(2) },
                    { assertThat(results.first { it.productId == 100L }.year).isEqualTo(2026) },
                    { assertThat(results.first { it.productId == 100L }.month).isEqualTo(4) },
                    { assertThat(results.first { it.productId == 100L }.rankNumber).isEqualTo(1) },
                    { assertThat(results.first { it.productId == 101L }.rankNumber).isEqualTo(2) },
                    { assertThat(results.first { it.productId == 100L }.viewCount).isEqualTo(300) },
                    { assertThat(results.first { it.productId == 100L }.likeCount).isEqualTo(60) },
                    { assertThat(results.first { it.productId == 100L }.unitsSold).isEqualTo(30) },
                    { assertThat(results.first { it.productId == 100L }.salesAmount).isEqualTo(300_000L) },
                )
            }

            @Test
            @DisplayName("101개 상품 데이터 중 TOP 100만 MV에 적재된다 (LIMIT 경계)")
            fun aggregate_limitTop100() {
                val baseDate = LocalDate.of(2026, 4, 16)
                // productId 1..101. productId가 높을수록 view_count 높음 → totalScore 높음.
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
                val results = monthlyProductRankJpaRepository.findAll()
                assertAll(
                    { assertThat(results).hasSize(100) },
                    { assertThat(results.first { it.rankNumber == 1 }.productId).isEqualTo(101L) },
                    { assertThat(results.first { it.rankNumber == 100 }.productId).isEqualTo(2L) },
                    { assertThat(results.none { it.productId == 1L }).isTrue() },
                )
            }

            @Test
            @DisplayName("totalScore 동점 상품은 product_id ASC 로 tie-break하여 rank가 결정된다")
            fun aggregate_tieBreakByProductIdAsc() {
                val baseDate = LocalDate.of(2026, 4, 16)
                insertDaily(productId = 200L, date = baseDate, view = 50, like = 0, units = 0, amount = 0L, orderScore = 0.0)
                insertDaily(productId = 100L, date = baseDate, view = 50, like = 0, units = 0, amount = 0L, orderScore = 0.0)

                jobLauncherTestUtils.job = job
                jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                val results = monthlyProductRankJpaRepository.findAll()
                assertAll(
                    { assertThat(results.first { it.productId == 100L }.rankNumber).isEqualTo(1) },
                    { assertThat(results.first { it.productId == 200L }.rankNumber).isEqualTo(2) },
                )
            }

            @Test
            @DisplayName("같은 (year, month)로 재실행하면 기존 MV 데이터를 교체한다 (멱등성)")
            fun aggregate_idempotent() {
                val baseDate = LocalDate.of(2026, 4, 16)
                insertDaily(productId = 100L, date = baseDate, view = 10, like = 2, units = 1, amount = 10_000L, orderScore = 6.45)

                jobLauncherTestUtils.job = job
                val params1 = JobParametersBuilder()
                    .addString("baseDate", "2026-04-16")
                    .addLong("run.id", 1L)
                    .toJobParameters()
                jobLauncherTestUtils.launchJob(params1)

                val params2 = JobParametersBuilder()
                    .addString("baseDate", "2026-04-16")
                    .addLong("run.id", 2L)
                    .toJobParameters()
                val jobExecution = jobLauncherTestUtils.launchJob(params2)

                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                assertThat(monthlyProductRankJpaRepository.findAll()).hasSize(1)
            }

            @Test
            @DisplayName("인접 월(3월/5월) 데이터는 집계에서 배제된다")
            fun aggregate_excludesOutOfRangeDates() {
                // 범위 밖: 3월 말일(2026-03-31), 5월 1일(2026-05-01) — 기간 경계 off-by-one 검증
                insertDaily(productId = 900L, date = LocalDate.of(2026, 3, 31), view = 1_000, like = 0, units = 0, amount = 0L, orderScore = 0.0)
                insertDaily(productId = 901L, date = LocalDate.of(2026, 5, 1), view = 1_000, like = 0, units = 0, amount = 0L, orderScore = 0.0)
                // 범위 안: 4월 데이터
                insertDaily(productId = 100L, date = LocalDate.of(2026, 4, 15), view = 10, like = 0, units = 0, amount = 0L, orderScore = 0.0)

                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
                val results = monthlyProductRankJpaRepository.findAll()
                assertAll(
                    { assertThat(results).hasSize(1) },
                    { assertThat(results).extracting<Long> { it.productId }.containsExactly(100L) },
                )
            }

            @Test
            @DisplayName("다른 (year, month) MV 행은 DELETE 범위 밖이라 보존된다")
            fun delete_preservesOtherMonths() {
                // arrange: 3월분 MV 행을 미리 저장 (과거 실행 결과 가정)
                monthlyProductRankJpaRepository.save(
                    MonthlyProductRankEntity(
                        productId = 900L,
                        year = 2026,
                        month = 3,
                        totalScore = 99.0,
                        rankNumber = 1,
                        viewCount = 100,
                        likeCount = 0,
                        unitsSold = 0,
                        salesAmount = 0L,
                        orderScore = 0.0,
                    ),
                )
                // 4월 daily 데이터
                insertDaily(productId = 100L, date = LocalDate.of(2026, 4, 15), view = 10, like = 0, units = 0, amount = 0L, orderScore = 0.0)

                jobLauncherTestUtils.job = job
                jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                // assert: 3월 행은 건드리지 않고, 4월 행만 새로 추가
                val marchRows = monthlyProductRankJpaRepository.findByYearAndMonthOrderByRankNumber(2026, 3)
                val aprilRows = monthlyProductRankJpaRepository.findByYearAndMonthOrderByRankNumber(2026, 4)
                assertAll(
                    { assertThat(marchRows).hasSize(1) },
                    { assertThat(marchRows.first().productId).isEqualTo(900L) },
                    { assertThat(aprilRows).hasSize(1) },
                    { assertThat(aprilRows.first().productId).isEqualTo(100L) },
                )
            }

            @Test
            @DisplayName("해당 월에 daily 데이터가 없어도 예외 없이 COMPLETED로 종료한다 (빈 월)")
            fun aggregate_emptyMonth() {
                // 4월 daily 데이터 없음. 범위 밖(3월)에만 데이터 존재.
                insertDaily(productId = 900L, date = LocalDate.of(2026, 3, 15), view = 10, like = 0, units = 0, amount = 0L, orderScore = 0.0)

                jobLauncherTestUtils.job = job
                val jobExecution = jobLauncherTestUtils.launchJob(uniqueParams(baseDateStr = "2026-04-16"))

                assertAll(
                    { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
                    { assertThat(monthlyProductRankJpaRepository.countByYearAndMonth(2026, 4)).isEqualTo(0L) },
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
    }
