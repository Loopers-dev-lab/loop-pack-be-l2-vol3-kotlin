package com.loopers.application.ranking

import com.loopers.zset.RedisZSetTemplate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.log10

class RankingScoreAccumulatorTest {

    private val redisZSetTemplate: RedisZSetTemplate = mockk(relaxed = true)
    private val rankingProperties = RankingProperties(
        weight = RankingProperties.Weight(view = 0.1, like = 0.2, order = 0.6),
        ttlDays = 2,
        carryOverWeight = 0.1,
    )
    private val accumulator = RankingScoreAccumulator(redisZSetTemplate, rankingProperties)

    @DisplayName("좋아요 점수")
    @Nested
    inner class LikeScore {

        @DisplayName("좋아요 추가 시 weight 0.2만큼 증가한다")
        @Test
        fun addLikeScore() {
            accumulator.addLikeScore(101L)

            verify { redisZSetTemplate.incrementScore(any(), "101", 0.2) }
        }

        @DisplayName("좋아요 취소 시 weight 0.2만큼 감소한다")
        @Test
        fun cancelLikeScore() {
            accumulator.cancelLikeScore(101L)

            verify { redisZSetTemplate.incrementScore(any(), "101", -0.2) }
        }
    }

    @DisplayName("주문 점수")
    @Nested
    inner class OrderScore {

        @DisplayName("주문 생성 시 log10 정규화된 점수가 반영된다")
        @Test
        fun addOrderScore() {
            accumulator.addOrderScore(listOf(101L), 10000L)

            val expectedIncrement = 0.6 * log10(1.0 + 10000)
            verify { redisZSetTemplate.incrementScore(any(), "101", expectedIncrement) }
        }

        @DisplayName("다중 상품 주문 시 상품별로 균등 분배된 금액 기준으로 점수가 반영된다")
        @Test
        fun addOrderScoreMultipleProducts() {
            accumulator.addOrderScore(listOf(101L, 202L), 20000L)

            val revenuePerProduct = 20000L / 2
            val expectedIncrement = 0.6 * log10(1.0 + revenuePerProduct)
            verify { redisZSetTemplate.incrementScore(any(), "101", expectedIncrement) }
            verify { redisZSetTemplate.incrementScore(any(), "202", expectedIncrement) }
        }

        @DisplayName("상품 목록이 비어있으면 아무것도 하지 않는다")
        @Test
        fun emptyProductIds() {
            accumulator.addOrderScore(emptyList(), 10000L)

            verify(exactly = 0) { redisZSetTemplate.incrementScore(any(), any(), any()) }
        }
    }

    @DisplayName("조회 점수")
    @Nested
    inner class ViewScore {

        @DisplayName("누적 조회수만큼 한 번에 반영된다")
        @Test
        fun addViewScore() {
            accumulator.addViewScore(101L, 50)

            verify { redisZSetTemplate.incrementScore(any(), "101", 0.1 * 50) }
        }

        @DisplayName("조회수가 0이면 아무것도 하지 않는다")
        @Test
        fun zeroCount() {
            accumulator.addViewScore(101L, 0)

            verify(exactly = 0) { redisZSetTemplate.incrementScore(any(), any(), any()) }
        }
    }

    @DisplayName("log 정규화 효과")
    @Nested
    inner class LogNormalization {

        @DisplayName("1000원과 100만원 주문의 점수 차이가 2배 이내이다")
        @Test
        fun logNormalizationPreventsHighPriceDomination() {
            val score1000 = 0.6 * log10(1.0 + 1000)
            val score1000000 = 0.6 * log10(1.0 + 1000000)

            assertThat(score1000000 / score1000).isLessThan(2.0)
        }
    }
}
