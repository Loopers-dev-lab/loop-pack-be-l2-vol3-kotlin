package com.loopers.application.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RankingScoreCalculatorTest {
    private val rankingScoreCalculator = RankingScoreCalculator()

    @Test
    fun `주문_점수는_수량만큼_반영된다`() {
        assertThat(rankingScoreCalculator.ordered(3L)).isEqualTo(3.0)
    }

    @Test
    fun `주문_1건은_좋아요_3건보다_높은_점수다`() {
        val orderScore = rankingScoreCalculator.ordered(1L)
        val likeScore = rankingScoreCalculator.likeChanged(3L)

        assertThat(orderScore).isGreaterThan(likeScore)
    }

    @Test
    fun `좋아요_취소는_음수_점수로_반영된다`() {
        assertThat(rankingScoreCalculator.likeChanged(-1L)).isEqualTo(-0.2)
    }
}
