package com.loopers.job.ranking

import com.loopers.batch.job.ranking.ProductRankingAggregationJobConfig
import com.loopers.infrastructure.metrics.ProductMetricsEntity
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.infrastructure.ranking.MonthlyProductRankingJpaRepository
import com.loopers.infrastructure.ranking.WeeklyProductRankingEntity
import com.loopers.infrastructure.ranking.WeeklyProductRankingJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "spring.batch.job.name=${ProductRankingAggregationJobConfig.JOB_NAME}",
        "spring.batch.job.enabled=false",
    ],
)
class ProductRankingAggregationJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(ProductRankingAggregationJobConfig.JOB_NAME) private val job: Job,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val weeklyProductRankingJpaRepository: WeeklyProductRankingJpaRepository,
    private val monthlyProductRankingJpaRepository: MonthlyProductRankingJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        @Suppress("unused")
        private val mysqlTestContainersConfig = MySqlTestContainersConfig()

        @Suppress("unused")
        private val redisTestContainersConfig = RedisTestContainersConfig()

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("datasource.mysql-jpa.main.jdbc-url") {
                System.getProperty("datasource.mysql-jpa.main.jdbc-url")
            }
            registry.add("datasource.mysql-jpa.main.username") {
                System.getProperty("datasource.mysql-jpa.main.username")
            }
            registry.add("datasource.mysql-jpa.main.password") {
                System.getProperty("datasource.mysql-jpa.main.password")
            }
            registry.add("datasource.redis.database") {
                System.getProperty("datasource.redis.database")
            }
            registry.add("datasource.redis.master.host") {
                System.getProperty("datasource.redis.master.host")
            }
            registry.add("datasource.redis.master.port") {
                System.getProperty("datasource.redis.master.port")
            }
            registry.add("datasource.redis.replicas[0].host") {
                System.getProperty("datasource.redis.replicas[0].host")
            }
            registry.add("datasource.redis.replicas[0].port") {
                System.getProperty("datasource.redis.replicas[0].port")
            }
        }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `targetDate_파라미터가_없으면_배치는_실패한다`() {
        jobLauncherTestUtils.job = job

        val jobExecution = jobLauncherTestUtils.launchJob()

        assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
    }

    @Test
    fun `배치는_주간과_월간_MV를_동시에_갱신한다`() {
        jobLauncherTestUtils.job = job
        seedProductMetrics()
        weeklyProductRankingJpaRepository.save(
            WeeklyProductRankingEntity(
                weekStartDate = LocalDate.parse("2026-04-13"),
                weekEndDate = LocalDate.parse("2026-04-19"),
                productId = 999L,
                ranking = 1L,
                score = 99.0,
                likeCount = 0L,
                viewCount = 0L,
                salesCount = 99L,
            ),
        )

        val jobExecution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString(ProductRankingAggregationJobConfig.TARGET_DATE_PARAMETER, "20260416")
                .toJobParameters(),
        )

        assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)

        val weekly = weeklyProductRankingJpaRepository.findAll().sortedBy { it.ranking }
        assertThat(weekly.map { it.productId }).containsExactly(2L, 1L)
        assertThat(weekly.map { it.ranking }).containsExactly(1L, 2L)
        assertThat(weekly.map { it.score }).containsExactly(5.5, 4.2)

        val monthly = monthlyProductRankingJpaRepository.findAll().sortedBy { it.ranking }
        assertThat(monthly.map { it.productId }).containsExactly(1L, 2L)
        assertThat(monthly.map { it.ranking }).containsExactly(1L, 2L)
        assertThat(monthly.map { it.score }).containsExactly(6.0, 5.5)
    }

    private fun seedProductMetrics() {
        productMetricsJpaRepository.saveAll(
            listOf(
                ProductMetricsEntity(
                    metricDate = LocalDate.parse("2026-04-01"),
                    productId = 1L,
                    likeCount = 4L,
                    viewCount = 10L,
                    salesCount = 0L,
                ),
                ProductMetricsEntity(
                    metricDate = LocalDate.parse("2026-04-13"),
                    productId = 1L,
                    likeCount = 1L,
                    viewCount = 10L,
                    salesCount = 3L,
                ),
                ProductMetricsEntity(
                    metricDate = LocalDate.parse("2026-04-15"),
                    productId = 2L,
                    likeCount = 0L,
                    viewCount = 5L,
                    salesCount = 5L,
                ),
                ProductMetricsEntity(
                    metricDate = LocalDate.parse("2026-03-31"),
                    productId = 3L,
                    likeCount = 10L,
                    viewCount = 100L,
                    salesCount = 10L,
                ),
            ),
        )
    }
}
