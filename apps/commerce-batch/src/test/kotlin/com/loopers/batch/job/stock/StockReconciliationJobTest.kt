package com.loopers.batch.job.stock

import com.loopers.config.redis.RedisKeys
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
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

@SpringBatchTest
@SpringBootTest
@TestPropertySource(properties = ["spring.batch.job.name=stockReconciliationJob"])
class StockReconciliationJobTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @Qualifier(StockReconciliationJobConfig.JOB_NAME) private val job: Job,
    private val redisTemplate: RedisTemplate<String, String>,
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun insertProduct(id: Long, stock: Int) {
        transactionTemplate.executeWithoutResult {
            entityManager.createNativeQuery(
                "INSERT INTO products (id, name, price, likes, stock_quantity, brand_id, created_at, updated_at) " +
                    "VALUES (:id, '상품', 1000, 0, :stock, 1, NOW(), NOW())",
            )
                .setParameter("id", id)
                .setParameter("stock", stock)
                .executeUpdate()
        }
    }

    @Test
    @DisplayName("Redis-DB 불일치 시 DB 기준으로 Redis가 보정된다")
    fun reconcilesRedisWithDb() {
        // given
        insertProduct(1L, 50)
        insertProduct(2L, 30)
        redisTemplate.opsForValue().set(RedisKeys.stockKey(1L), "45")
        redisTemplate.opsForValue().set(RedisKeys.stockKey(2L), "30")

        // when
        jobLauncherTestUtils.job = job
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters(),
        )

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        assertThat(redisTemplate.opsForValue().get(RedisKeys.stockKey(1L))).isEqualTo("50")
        assertThat(redisTemplate.opsForValue().get(RedisKeys.stockKey(2L))).isEqualTo("30")
    }

    @Test
    @DisplayName("Redis에 키가 없는 상품도 DB 기준으로 생성된다")
    fun createsRedisKey_whenMissing() {
        // given
        insertProduct(1L, 100)

        // when
        jobLauncherTestUtils.job = job
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters(),
        )

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        assertThat(redisTemplate.opsForValue().get(RedisKeys.stockKey(1L))).isEqualTo("100")
    }
}
