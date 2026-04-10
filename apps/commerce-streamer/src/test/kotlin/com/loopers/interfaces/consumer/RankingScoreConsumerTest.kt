package com.loopers.interfaces.consumer

import com.loopers.config.RankingProperties
import com.loopers.config.redis.RedisConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.utils.RedisCleanUp
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.log10

@SpringBootTest(
    classes = [
        RedisTestContainersConfig::class,
        RedisConfig::class,
        RedisCleanUp::class,
        RankingScoreConsumer::class,
        RankingProperties::class,
        ObjectMapper::class,
    ],
)
@DisplayName("RankingScoreConsumer 통합 테스트")
class RankingScoreConsumerTest @Autowired constructor(
    private val consumer: RankingScoreConsumer,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val objectMapper = ObjectMapper()
    private val now = LocalDateTime.now()
    private val dailyKey = "ranking:all:${now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}"
    private val hourlyKey = "ranking:all:${now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}:${now.format(DateTimeFormatter.ofPattern("HH"))}"

    private val noopAck = object : Acknowledgment {
        override fun acknowledge() {}
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun buildRecord(actionType: String, targetId: Long, metadata: Map<String, Any> = emptyMap()): ConsumerRecord<String, ByteArray> {
        val payload = mutableMapOf<String, Any>(
            "actionType" to actionType,
            "targetId" to targetId,
        )
        payload.putAll(metadata)
        return ConsumerRecord("product.action", 0, 0, null, objectMapper.writeValueAsBytes(payload))
    }

    @Nested
    @DisplayName("점수 갱신")
    inner class ScoreUpdate {

        @Test
        @DisplayName("VIEW 이벤트는 0.1점을 누적한다")
        fun `VIEW 점수 반영`() {
            // Arrange
            val records = listOf(
                buildRecord("VIEW", 101),
                buildRecord("VIEW", 101),
                buildRecord("VIEW", 101),
            )

            // Act
            consumer.onProductActions(records, noopAck)

            // Assert
            val score = redisTemplate.opsForZSet().score(dailyKey, "101")
            assertThat(score).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("LIKE 이벤트는 0.2점을 누적한다")
        fun `LIKE 점수 반영`() {
            val records = listOf(buildRecord("LIKE", 202))

            consumer.onProductActions(records, noopAck)

            val score = redisTemplate.opsForZSet().score(dailyKey, "202")
            assertThat(score).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("ORDER 이벤트는 0.7 * log10(price * quantity + 1) 점을 누적한다")
        fun `ORDER 점수 log10 반영`() {
            val records = listOf(
                buildRecord("ORDER", 303, mapOf("price" to 10000L, "quantity" to 2)),
            )

            consumer.onProductActions(records, noopAck)

            val expected = 0.7 * log10(10000.0 * 2 + 1)
            val score = redisTemplate.opsForZSet().score(dailyKey, "303")
            assertThat(score).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("배치 집계")
    inner class BatchAggregation {

        @Test
        @DisplayName("같은 상품의 VIEW 이벤트가 배치 내에서 합산되어 1번만 flush된다")
        fun `인메모리 집계 후 flush`() {
            // Arrange: 상품 101에 VIEW 5건, 상품 202에 VIEW 3건
            val records = (1..5).map { buildRecord("VIEW", 101) } +
                (1..3).map { buildRecord("VIEW", 202) }

            // Act
            consumer.onProductActions(records, noopAck)

            // Assert
            val score101 = redisTemplate.opsForZSet().score(dailyKey, "101")
            val score202 = redisTemplate.opsForZSet().score(dailyKey, "202")
            assertThat(score101).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001))
            assertThat(score202).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("일간 키와 시간 키에 동시에 점수가 반영된다")
        fun `이중 키 동시 갱신`() {
            val records = listOf(buildRecord("VIEW", 101))

            consumer.onProductActions(records, noopAck)

            val dailyScore = redisTemplate.opsForZSet().score(dailyKey, "101")
            val hourlyScore = redisTemplate.opsForZSet().score(hourlyKey, "101")
            assertThat(dailyScore).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.001))
            assertThat(hourlyScore).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("에러 처리")
    inner class ErrorHandling {

        @Test
        @DisplayName("파싱 불가능한 레코드는 무시하고 나머지를 처리한다")
        fun `잘못된 레코드 무시`() {
            val badRecord = ConsumerRecord<String, ByteArray>("product.action", 0, 0, null, "invalid-json".toByteArray())
            val goodRecord = buildRecord("VIEW", 101)

            consumer.onProductActions(listOf(badRecord, goodRecord), noopAck)

            val score = redisTemplate.opsForZSet().score(dailyKey, "101")
            assertThat(score).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.001))
        }
    }
}
