package com.loopers.batch.job.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.config.redis.RedisKeys
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBatchTest
@SpringBootTest
@TestPropertySource(properties = ["spring.batch.job.name=rankingReconciliationJob"])
class RankingReconciliationJobTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @Qualifier(RankingReconciliationJobConfig.JOB_NAME) private val job: Job,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    private val todayKey = RedisKeys.rankingKey(
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
    )

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun insertProductAndMetrics(productId: Long, viewCount: Long, likeCount: Long, salesCount: Long) {
        transactionTemplate.executeWithoutResult {
            entityManager.createNativeQuery(
                "INSERT INTO products (id, name, price, likes, stock_quantity, brand_id, created_at, updated_at) " +
                    "VALUES (:id, '상품', 1000, 0, 100, 1, NOW(), NOW())",
            )
                .setParameter("id", productId)
                .executeUpdate()

            entityManager.createNativeQuery(
                "INSERT INTO product_metrics (product_id, view_count, like_count, sales_count, version, updated_at) " +
                    "VALUES (:productId, :viewCount, :likeCount, :salesCount, 1, NOW())",
            )
                .setParameter("productId", productId)
                .setParameter("viewCount", viewCount)
                .setParameter("likeCount", likeCount)
                .setParameter("salesCount", salesCount)
                .executeUpdate()
        }
    }

    private fun launchJob() = jobLauncherTestUtils.also { it.job = job }.launchJob(
        JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters(),
    )

    @Test
    @DisplayName("DB-Redis 점수 불일치 시 DB 기준으로 Redis가 보정된다")
    fun reconcilesRedisWithDb() {
        // given — DB: view=10, like=5, sales=3 → 점수 = 10×0.1 + 5×0.2 + 3×0.7 = 4.1
        insertProductAndMetrics(1L, viewCount = 10, likeCount = 5, salesCount = 3)
        redisTemplate.opsForZSet().add(todayKey, "1", 999.0) // 잘못된 점수

        // when
        val execution = launchJob()

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        val score = redisTemplate.opsForZSet().score(todayKey, "1")
        assertThat(score).isNotNull()
        assertThat(score!!).isCloseTo(4.1, Offset.offset(0.001))
    }

    @Test
    @DisplayName("Redis에 키가 없는 상품도 DB 기준으로 점수가 생성된다")
    fun createsScoreWhenRedisMissing() {
        // given — Redis에 아무 데이터 없음
        insertProductAndMetrics(1L, viewCount = 20, likeCount = 10, salesCount = 5)

        // when
        val execution = launchJob()

        // then — 점수 = 20×0.1 + 10×0.2 + 5×0.7 = 7.5
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        val score = redisTemplate.opsForZSet().score(todayKey, "1")
        assertThat(score).isNotNull()
        assertThat(score!!).isCloseTo(7.5, Offset.offset(0.001))
    }

    @Test
    @DisplayName("DB-Redis 점수가 일치하면 Redis를 갱신하지 않는다")
    fun skipsWhenScoresMatch() {
        // given — DB 기준 점수 = 10×0.1 + 5×0.2 + 3×0.7 = 4.1
        insertProductAndMetrics(1L, viewCount = 10, likeCount = 5, salesCount = 3)
        redisTemplate.opsForZSet().add(todayKey, "1", 4.1)

        // when
        val execution = launchJob()

        // then — 일치하므로 Writer에 전달되지 않음 (writeCount = 0)
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        val stepExecution = execution.stepExecutions.first()
        assertThat(stepExecution.writeCount).isEqualTo(0)
    }
}
