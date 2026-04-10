package com.loopers.e2e

import com.loopers.config.redis.RedisConfig
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.support.EmbeddedKafkaTestSupport
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class OrderEventE2ETest @Autowired constructor(
    private val productMetricsRepository: ProductMetricsRepository,
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : EmbeddedKafkaTestSupport() {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun insertProduct(id: Long, stock: Int) {
        transactionTemplate.executeWithoutResult {
            entityManager.createNativeQuery(
                "INSERT INTO products (id, name, price, likes, stock_quantity, brand_id, created_at, updated_at) VALUES (:id, '상품', 1000, 0, :stock, 1, NOW(), NOW())",
            )
                .setParameter("id", id)
                .setParameter("stock", stock)
                .executeUpdate()
        }
    }

    private fun getProductStock(productId: Long): Int? {
        return transactionTemplate.execute {
            val result = entityManager.createNativeQuery(
                "SELECT stock_quantity FROM products WHERE id = :id",
            )
                .setParameter("id", productId)
                .singleResult
            (result as Number?)?.toInt()
        }
    }

    @DisplayName("주문 완료 이벤트 E2E:")
    @Nested
    inner class OrderCompletedEventFlow {

        @DisplayName("주문 완료 이벤트가 Kafka를 통해 salesCount와 랭킹 ZSET 점수에 반영된다.")
        @Test
        fun orderCompletedEventUpdatesSalesCountAndRankingScore() {
            // arrange
            waitForConsumerAssignment()
            val payload = """{"orderId":1,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"상품A","unitPrice":10000},{"productId":200,"quantity":3,"productName":"상품B","unitPrice":10000}],"couponId":null,"totalAmount":50000,"paymentAmount":50000}"""
            val envelope = EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = "ORDER_COMPLETED",
                aggregateId = "1",
                version = System.currentTimeMillis(),
                timestamp = Instant.now(),
                payload = payload,
            )

            // act
            sendEnvelope("order-events", envelope)

            // assert — salesCount 반영 확인
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted {
                val metricsA = productMetricsRepository.findByProductId(100L)
                assertThat(metricsA).isNotNull()
                assertThat(metricsA!!.salesCount).isEqualTo(2)

                val metricsB = productMetricsRepository.findByProductId(200L)
                assertThat(metricsB).isNotNull()
                assertThat(metricsB!!.salesCount).isEqualTo(3)
            }

            // assert — Redis ZSET 랭킹 점수 반영 확인
            val todayKey = RedisKeys.rankingKey(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
            val scoreA = redisTemplate.opsForZSet().score(todayKey, "100")
            val scoreB = redisTemplate.opsForZSet().score(todayKey, "200")

            // 상품A: 0.7 × log10(10000 × 2) = 0.7 × log10(20000)
            val expectedScoreA = 0.7 * kotlin.math.log10(10000.0 * 2)
            // 상품B: 0.7 × log10(10000 × 3) = 0.7 × log10(30000)
            val expectedScoreB = 0.7 * kotlin.math.log10(10000.0 * 3)

            assertThat(scoreA).isNotNull()
            assertThat(scoreA!!).isCloseTo(expectedScoreA, Offset.offset(0.001))
            assertThat(scoreB).isNotNull()
            assertThat(scoreB!!).isCloseTo(expectedScoreB, Offset.offset(0.001))
        }

        @DisplayName("주문 완료 이벤트가 Kafka를 통해 DB 재고를 차감한다.")
        @Test
        fun orderCompletedEventDecrementsDbStock() {
            // arrange
            waitForConsumerAssignment()
            insertProduct(100L, 50)
            insertProduct(200L, 30)

            val payload = """{"orderId":2,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"상품A","unitPrice":10000},{"productId":200,"quantity":3,"productName":"상품B","unitPrice":10000}],"couponId":null,"totalAmount":50000,"paymentAmount":50000}"""
            val envelope = EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = "ORDER_COMPLETED",
                aggregateId = "2",
                version = System.currentTimeMillis(),
                timestamp = Instant.now(),
                payload = payload,
            )

            // act
            sendEnvelope("order-events", envelope)

            // assert
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted {
                assertThat(getProductStock(100L)).isEqualTo(48)
                assertThat(getProductStock(200L)).isEqualTo(27)
            }
        }
    }
}
