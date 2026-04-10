package com.loopers.domain.catalog

import com.loopers.config.kafka.message.OrderItemMessage
import com.loopers.config.kafka.message.OrderMessage
import com.loopers.config.kafka.message.ProductLikedMessage
import com.loopers.config.kafka.message.ProductViewedMessage
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.order.OrderEventService
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@SpringBootTest
class ProductEventServiceIntegrationTest @Autowired constructor(
    private val productEventService: ProductEventService,
    private val orderEventService: OrderEventService,
    @param:Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val KEY_PREFIX = "rank"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val TARGET_DATE: LocalDate = LocalDate.of(2026, 4, 7)
        private val OCCURRED_AT: ZonedDateTime = TARGET_DATE.atStartOfDay(ZoneId.of("Asia/Seoul"))

        // ProductRankProperties 기본값
        private const val WEIGHT_VIEW = 0.1
        private const val WEIGHT_LIKE = 0.2
        private const val WEIGHT_ORDER = 0.7
        private const val FLOAT_TOLERANCE = 1e-9
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisTemplate.keys("$KEY_PREFIX:*")?.forEach { redisTemplate.delete(it) }
    }

    private fun keyOf(type: String, date: LocalDate = TARGET_DATE): String =
        "$KEY_PREFIX:$type:${date.format(DATE_FORMAT)}"

    private fun scoreOf(type: String, productId: Long, date: LocalDate = TARGET_DATE): Double? =
        redisTemplate.opsForZSet().score(keyOf(type, date), productId.toString())

    @Nested
    @DisplayName("이벤트 → ZSET 적재")
    inner class EventToZSet {

        @DisplayName("상품 조회 이벤트가 처리되면 rank:view에 +1, rank:all에 view 가중치만큼 누적된다.")
        @Test
        fun shouldIncrementViewZSet() {
            // arrange
            val productId = 101L
            val message = ProductViewedMessage(
                eventId = UUID.randomUUID().toString(),
                userId = 1L,
                productId = productId,
                occurredAt = OCCURRED_AT,
            )

            // act
            productEventService.handleProductViewed(message)

            // assert
            assertAll(
                { assertThat(scoreOf("view", productId)!!).isCloseTo(1.0, within(FLOAT_TOLERANCE)) },
                { assertThat(scoreOf("all", productId)!!).isCloseTo(WEIGHT_VIEW, within(FLOAT_TOLERANCE)) },
            )
        }

        @DisplayName("상품 좋아요 이벤트가 처리되면 rank:like에 +1, rank:all에 like 가중치만큼 누적된다.")
        @Test
        fun shouldIncrementLikeZSet() {
            // arrange
            val productId = 102L
            val message = ProductLikedMessage(
                eventId = UUID.randomUUID().toString(),
                userId = 1L,
                productId = productId,
                occurredAt = OCCURRED_AT,
            )

            // act
            productEventService.handleProductLiked(message)

            // assert
            assertAll(
                { assertThat(scoreOf("like", productId)!!).isCloseTo(1.0, within(FLOAT_TOLERANCE)) },
                { assertThat(scoreOf("all", productId)!!).isCloseTo(WEIGHT_LIKE, within(FLOAT_TOLERANCE)) },
            )
        }

        @DisplayName("주문 이벤트가 처리되면 items의 productId별로 quantity만큼 rank:order/all에 누적된다.")
        @Test
        fun shouldIncrementOrderZSetPerItem() {
            // arrange
            val productAId = 201L
            val productBId = 202L
            val message = OrderMessage(
                eventId = UUID.randomUUID().toString(),
                orderId = 999L,
                userId = 1L,
                totalPrice = BigDecimal("50000"),
                items = listOf(
                    OrderItemMessage(productId = productAId, quantity = 2, price = BigDecimal("10000")),
                    OrderItemMessage(productId = productBId, quantity = 1, price = BigDecimal("30000")),
                ),
                occurredAt = OCCURRED_AT,
            )

            // act
            orderEventService.handleOrderCreated(message)

            // assert
            assertAll(
                { assertThat(scoreOf("order", productAId)!!).isCloseTo(2.0, within(FLOAT_TOLERANCE)) },
                { assertThat(scoreOf("all", productAId)!!).isCloseTo(WEIGHT_ORDER * 2, within(FLOAT_TOLERANCE)) },
                { assertThat(scoreOf("order", productBId)!!).isCloseTo(1.0, within(FLOAT_TOLERANCE)) },
                { assertThat(scoreOf("all", productBId)!!).isCloseTo(WEIGHT_ORDER, within(FLOAT_TOLERANCE)) },
            )
        }

        @DisplayName("같은 eventId가 2번 들어오면 멱등하게 처리되어 ZSET에 한 번만 반영된다.")
        @Test
        fun shouldBeIdempotentForSameEventId() {
            // arrange
            val productId = 301L
            val sameEventId = UUID.randomUUID().toString()
            val message = ProductViewedMessage(
                eventId = sameEventId,
                userId = 1L,
                productId = productId,
                occurredAt = OCCURRED_AT,
            )

            // act
            productEventService.handleProductViewed(message)
            productEventService.handleProductViewed(message)

            // assert — 한 번만 반영
            assertThat(scoreOf("view", productId)!!).isCloseTo(1.0, within(FLOAT_TOLERANCE))
        }
    }

    @Nested
    @DisplayName("가중치 적용 검증")
    inner class WeightVerification {

        @DisplayName("주문 1건(0.7) > 좋아요 3건(0.6) — 정확한 score와 ZREVRANGE 순위로 검증")
        @Test
        fun orderOneBeatsLikeThreeWithExactScoreAndRank() {
            // arrange — 주문 1건 받는 상품
            val orderProductId = 401L
            orderEventService.handleOrderCreated(
                OrderMessage(
                    eventId = UUID.randomUUID().toString(),
                    orderId = 1L,
                    userId = 1L,
                    totalPrice = BigDecimal("10000"),
                    items = listOf(OrderItemMessage(orderProductId, 1, BigDecimal("10000"))),
                    occurredAt = OCCURRED_AT,
                ),
            )

            // arrange — 좋아요 3건 받는 상품
            val likeProductId = 402L
            repeat(3) {
                productEventService.handleProductLiked(
                    ProductLikedMessage(
                        eventId = UUID.randomUUID().toString(),
                        userId = 1L,
                        productId = likeProductId,
                        occurredAt = OCCURRED_AT,
                    ),
                )
            }

            // act — rank:all ZSET 전체를 ZREVRANGE로 점수 내림차순 조회
            val allKey = keyOf("all")
            val tuples = redisTemplate.opsForZSet().reverseRangeWithScores(allKey, 0, -1)?.toList().orEmpty()

            // assert
            val orderScore = scoreOf("all", orderProductId)!!
            val likeScore = scoreOf("all", likeProductId)!!
            val expectedOrderScore = WEIGHT_ORDER          // 0.7
            val expectedLikeScore = WEIGHT_LIKE * 3         // 0.6 (= 0.2 × 3)

            assertAll(
                // 1) 정확한 score 검증 — 부동소수점 누적 오차를 감안한 tolerance 사용
                { assertThat(orderScore).isCloseTo(expectedOrderScore, within(FLOAT_TOLERANCE)) },
                { assertThat(likeScore).isCloseTo(expectedLikeScore, within(FLOAT_TOLERANCE)) },
                // 2) 두 상품이 모두 ZSET에 적재되었는지
                { assertThat(tuples).hasSize(2) },
                // 3) ZREVRANGE 결과 순위: 주문 상품이 1등(0번 인덱스), 좋아요 상품이 2등(1번 인덱스)
                { assertThat(tuples[0].value).isEqualTo(orderProductId.toString()) },
                { assertThat(tuples[0].score!!).isCloseTo(expectedOrderScore, within(FLOAT_TOLERANCE)) },
                { assertThat(tuples[1].value).isEqualTo(likeProductId.toString()) },
                { assertThat(tuples[1].score!!).isCloseTo(expectedLikeScore, within(FLOAT_TOLERANCE)) },
                // 4) ZREVRANK로도 동일하게 검증 (0-based)
                {
                    assertThat(redisTemplate.opsForZSet().reverseRank(allKey, orderProductId.toString())!!)
                        .isEqualTo(0L)
                },
                {
                    assertThat(redisTemplate.opsForZSet().reverseRank(allKey, likeProductId.toString())!!)
                        .isEqualTo(1L)
                },
            )
        }
    }
}
