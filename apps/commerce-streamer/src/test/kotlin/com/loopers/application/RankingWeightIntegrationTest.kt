package com.loopers.application

import com.loopers.config.redis.RedisConfig
import com.loopers.config.redis.RedisKeys
import com.loopers.event.EventEnvelope
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest
@DisplayName("랭킹 가중치 통합 테스트")
class RankingWeightIntegrationTest @Autowired constructor(
    private val catalogEventProcessor: CatalogEventProcessor,
    private val orderEventProcessor: OrderEventProcessor,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val todayKey = RedisKeys.rankingKey(
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
    )

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun catalogEnvelope(
        eventId: String,
        eventType: String,
        productId: Long,
        version: Long,
    ) = EventEnvelope(
        eventId = eventId,
        eventType = eventType,
        aggregateId = productId.toString(),
        version = version,
        timestamp = Instant.now(),
        payload = """{"userId":1,"productId":$productId}""",
    )

    private fun orderEnvelope(
        eventId: String,
        productId: Long,
        unitPrice: Long,
        quantity: Int,
    ) = EventEnvelope(
        eventId = eventId,
        eventType = "ORDER_COMPLETED",
        aggregateId = "1",
        version = System.currentTimeMillis(),
        timestamp = Instant.now(),
        payload = """{"orderId":1,"userId":1,"items":[{"productId":$productId,"quantity":$quantity,"productName":"상품","unitPrice":$unitPrice}],"couponId":null,"totalAmount":${unitPrice * quantity},"paymentAmount":${unitPrice * quantity}}""",
    )

    @DisplayName("주문 1건(10000원)의 랭킹 점수가 좋아요 3건의 점수보다 높다.")
    @Test
    fun orderScoreExceedsThreeLikeScores() {
        // arrange
        val likedProductId = 100L
        val orderedProductId = 200L

        // act — 좋아요 3건 처리
        catalogEventProcessor.process(catalogEnvelope("evt-like-1", "LIKED", likedProductId, version = 1L))
        catalogEventProcessor.process(catalogEnvelope("evt-like-2", "LIKED", likedProductId, version = 2L))
        catalogEventProcessor.process(catalogEnvelope("evt-like-3", "LIKED", likedProductId, version = 3L))

        // act — 주문 1건 처리 (10000원 × 1개)
        orderEventProcessor.process(orderEnvelope("evt-order-1", orderedProductId, unitPrice = 10000L, quantity = 1))

        // assert — 주문 상품의 점수가 좋아요 상품보다 높다
        val likeScore = redisTemplate.opsForZSet().score(todayKey, likedProductId.toString())
        val orderScore = redisTemplate.opsForZSet().score(todayKey, orderedProductId.toString())

        assertThat(likeScore).isNotNull()
        assertThat(orderScore).isNotNull()
        assertThat(orderScore!!).isGreaterThan(likeScore!!)
    }

    @DisplayName("좋아요 → 좋아요 취소를 반복하면 점수가 대칭적으로 증감한다.")
    @Test
    fun likeAndUnlikeAreSymmetric() {
        // arrange
        val productId = 999L

        // act — 좋아요 3건 후 좋아요 취소 3건
        catalogEventProcessor.process(catalogEnvelope("evt-like-1", "LIKED", productId, version = 1L))
        catalogEventProcessor.process(catalogEnvelope("evt-like-2", "LIKED", productId, version = 2L))
        catalogEventProcessor.process(catalogEnvelope("evt-like-3", "LIKED", productId, version = 3L))
        catalogEventProcessor.process(catalogEnvelope("evt-unlike-1", "UNLIKED", productId, version = 4L))
        catalogEventProcessor.process(catalogEnvelope("evt-unlike-2", "UNLIKED", productId, version = 5L))
        catalogEventProcessor.process(catalogEnvelope("evt-unlike-3", "UNLIKED", productId, version = 6L))

        // assert — 점수가 0으로 복원
        val score = redisTemplate.opsForZSet().score(todayKey, productId.toString())
        assertThat(score).isNotNull()
        assertThat(score!!).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001))
    }
}
