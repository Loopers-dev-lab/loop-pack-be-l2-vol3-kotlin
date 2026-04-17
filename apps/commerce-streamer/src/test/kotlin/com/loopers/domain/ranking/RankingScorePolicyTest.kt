package com.loopers.domain.ranking

import com.loopers.config.kafka.event.CatalogEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RankingScorePolicy")
class RankingScorePolicyTest {

    private val policy = RankingScorePolicy(
        viewWeight = 0.1,
        likeWeight = 0.2,
        orderWeight = 0.7,
    )

    @DisplayName("조회 이벤트는 0.1의 가중치를 적용한다")
    @Test
    fun viewEventWeight() {
        val result = policy.calculateIncrement(CatalogEventType.PRODUCT_VIEWED, delta = 1)
        assertThat(result).isEqualTo(0.1)
    }

    @DisplayName("좋아요 이벤트는 0.2의 가중치를 적용한다")
    @Test
    fun likeEventWeight() {
        val result = policy.calculateIncrement(CatalogEventType.LIKE_CHANGED, delta = 1)
        assertThat(result).isEqualTo(0.2)
    }

    @DisplayName("주문 이벤트는 0.7의 가중치를 적용한다")
    @Test
    fun orderEventWeight() {
        val result = policy.calculateIncrement(CatalogEventType.ORDER_COMPLETED, delta = 1)
        assertThat(result).isEqualTo(0.7)
    }

    @DisplayName("delta가 음수이면 음수 점수를 반환한다 (좋아요 취소)")
    @Test
    fun negativeDeltaProducesNegativeScore() {
        val result = policy.calculateIncrement(CatalogEventType.LIKE_CHANGED, delta = -1)
        assertThat(result).isEqualTo(-0.2)
    }

    @DisplayName("delta를 가중치에 곱해서 반환한다")
    @Test
    fun multipliesDeltaByWeight() {
        val result = policy.calculateIncrement(CatalogEventType.ORDER_COMPLETED, delta = 3)
        assertThat(result).isCloseTo(2.1, org.assertj.core.data.Offset.offset(0.0001))
    }

    @DisplayName("주문 1건(0.7)은 좋아요 3건(0.6)보다 점수가 높다")
    @Test
    fun orderOutranksThreeLikes() {
        val orderScore = policy.calculateIncrement(CatalogEventType.ORDER_COMPLETED, delta = 1)
        val likesScore = policy.calculateIncrement(CatalogEventType.LIKE_CHANGED, delta = 1) * 3

        assertThat(orderScore).isGreaterThan(likesScore)
    }
}
