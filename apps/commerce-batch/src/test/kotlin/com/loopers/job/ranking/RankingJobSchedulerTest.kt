package com.loopers.job.ranking

import com.loopers.batch.ranking.entity.MvProductRankMonthlyBatchEntity
import com.loopers.batch.ranking.entity.MvProductRankWeeklyBatchEntity
import com.loopers.batch.ranking.entity.ProductMetricsDailyBatchEntity
import com.loopers.batch.scheduler.RankingJobScheduler
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RankingJobSchedulerTest.TestClockConfig::class)
@ActiveProfiles("scheduler", "test")
class RankingJobSchedulerTest @Autowired constructor(
    private val scheduler: RankingJobScheduler,
    private val databaseCleanUp: DatabaseCleanUp,
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) {
    @TestConfiguration
    class TestClockConfig {
        @Bean
        @Primary
        fun testClock(): Clock =
            Clock.fixed(
                ZonedDateTime.parse("2026-04-13T01:30:00+09:00").toInstant(),
                java.time.ZoneId.of("Asia/Seoul"),
            )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("scheduler 프로파일 활성 시 RankingJobScheduler 빈이 등록된다")
    @Test
    fun schedulerBeanIsPresentWhenSchedulerProfileIsActive() {
        assertThat(scheduler).isNotNull()
    }

    @DisplayName("WeeklyRankingJob 실행 시 지난 주(2026-W15) periodKey로 MV에 저장된다")
    @Test
    fun weeklyJobUsesLastWeekAsPeriodKey() {
        // arrange: Clock 고정(2026-04-13 월요일) → minusWeeks(1) → 2026-04-06 → 2026-W15
        transactionTemplate.execute {
            entityManager.persist(
                // metricDate: 2026-W15(Apr 6-12) 범위 내
                ProductMetricsDailyBatchEntity(
                    metricDate = LocalDate.of(2026, 4, 8),
                    productId = 1L,
                    viewCount = 10L,
                    likeCount = 0L,
                    salesCount = 0L,
                ),
            )
        }

        // act
        scheduler.runWeeklyRankingJob()

        // assert
        val results =
            transactionTemplate.execute {
                entityManager
                    .createQuery(
                        "SELECT m FROM MvProductRankWeeklyBatchEntity m",
                        MvProductRankWeeklyBatchEntity::class.java,
                    )
                    .resultList
            } ?: emptyList()
        assertAll(
            { assertThat(results).hasSize(1) },
            { assertThat(results[0].periodKey).isEqualTo("2026-W15") },
        )
    }

    @DisplayName("MonthlyRankingJob 실행 시 지난 달(2026-03) periodKey로 MV에 저장된다")
    @Test
    fun monthlyJobUsesLastMonthAsPeriodKey() {
        // arrange: Clock 고정(2026-04-13) → minusMonths(1) → 2026-03-13 → "2026-03"
        transactionTemplate.execute {
            entityManager.persist(
                // metricDate: 2026-03(Mar 1-31) 범위 내
                ProductMetricsDailyBatchEntity(
                    metricDate = LocalDate.of(2026, 3, 15),
                    productId = 1L,
                    viewCount = 10L,
                    likeCount = 0L,
                    salesCount = 0L,
                ),
            )
        }

        // act
        scheduler.runMonthlyRankingJob()

        // assert
        val results =
            transactionTemplate.execute {
                entityManager
                    .createQuery(
                        "SELECT m FROM MvProductRankMonthlyBatchEntity m",
                        MvProductRankMonthlyBatchEntity::class.java,
                    )
                    .resultList
            } ?: emptyList()
        assertAll(
            { assertThat(results).hasSize(1) },
            { assertThat(results[0].periodKey).isEqualTo("2026-03") },
        )
    }
}
