package com.loopers.interfaces.consumer

import com.loopers.config.redis.RedisRankingConstants
import com.loopers.domain.ranking.RankingWeight
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
    topics = [
        CatalogEventConsumer.TOPIC,
        OrderEventConsumer.TOPIC,
        CouponIssueConsumer.TOPIC,
        "demo.internal.topic-v1",
    ],
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://localhost:0", "port=0"],
)
@TestPropertySource(
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "demo-kafka.test.topic-name=demo.internal.topic-v1",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=catalog-event-integration-test",
    ],
)
@DirtiesContext
class CatalogEventIntegrationTest @Autowired constructor(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val clock: Clock,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry,
) {

    private val rankingKey: String
        get() {
            val today = LocalDate.now(clock)
            return "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
        }

    @BeforeEach
    fun setUp() {
        kafkaListenerEndpointRegistry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, 1)
        }
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("ProductViewed 이벤트 발행 시")
    inner class OnProductViewed {

        @Test
        @DisplayName("Redis ZSET에 VIEW 가중치가 반영된다")
        fun `Kafka에 ProductViewed 이벤트 발행 시 Redis ZSET에 VIEW 가중치가 반영된다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "PRODUCT_VIEWED",
                "productId" to 1L,
            )

            // Act
            kafkaTemplate.send(CatalogEventConsumer.TOPIC, payload).get()

            // Assert
            awaitRedisScore(productId = 1L, expectedScore = RankingWeight.VIEW)
        }
    }

    @Nested
    @DisplayName("PaymentCompleted 이벤트 발행 시")
    inner class OnPaymentCompleted {

        @Test
        @DisplayName("Redis ZSET에 ORDER × quantity 가중치가 반영된다")
        fun `Kafka에 PaymentCompleted 이벤트 발행 시 Redis ZSET에 ORDER × quantity 가중치가 반영된다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-2",
                "eventType" to "PAYMENT_COMPLETED",
                "productId" to 1L,
                "quantity" to 2L,
            )

            // Act
            kafkaTemplate.send(OrderEventConsumer.TOPIC, payload).get()

            // Assert
            awaitRedisScore(productId = 1L, expectedScore = RankingWeight.ORDER * 2)
        }
    }

    @Nested
    @DisplayName("복합 이벤트 순차 발행 시")
    inner class OnMultipleEvents {

        @Test
        @DisplayName("VIEW + LIKE + ORDER 이벤트의 누적 점수가 합산된다")
        fun `Kafka에 VIEW, LIKE, ORDER 이벤트를 순차 발행하면 누적 점수가 합산된다`() {
            // Arrange
            val viewPayload = mapOf(
                "eventId" to "evt-3",
                "eventType" to "PRODUCT_VIEWED",
                "productId" to 1L,
            )
            val likePayload = mapOf(
                "eventId" to "evt-4",
                "eventType" to "LIKE_ADDED",
                "productId" to 1L,
            )
            val orderPayload = mapOf(
                "eventId" to "evt-5",
                "eventType" to "PAYMENT_COMPLETED",
                "productId" to 1L,
                "quantity" to 1L,
            )

            // Act
            kafkaTemplate.send(CatalogEventConsumer.TOPIC, viewPayload).get()
            kafkaTemplate.send(CatalogEventConsumer.TOPIC, likePayload).get()
            kafkaTemplate.send(OrderEventConsumer.TOPIC, orderPayload).get()

            // Assert
            val expectedScore = RankingWeight.VIEW + RankingWeight.LIKE + RankingWeight.ORDER
            awaitRedisScore(productId = 1L, expectedScore = expectedScore)
        }
    }

    private fun awaitRedisScore(productId: Long, expectedScore: Double, timeoutMs: Long = 30000) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val score = redisTemplate.opsForZSet().score(rankingKey, productId.toString())
            if (score != null && Math.abs(score - expectedScore) < 0.001) return
            Thread.sleep(200)
        }
        val actual = redisTemplate.opsForZSet().score(rankingKey, productId.toString())
        assertThat(actual)
            .describedAs("productId=%s의 Redis 점수가 %s이어야 합니다", productId, expectedScore)
            .isNotNull()
        assertThat(actual!!).isCloseTo(expectedScore, Offset.offset(0.001))
    }
}
