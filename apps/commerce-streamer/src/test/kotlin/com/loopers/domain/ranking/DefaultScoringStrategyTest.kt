package com.loopers.domain.ranking

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("DefaultScoringStrategy 단위 테스트")
class DefaultScoringStrategyTest {

    private val strategy = DefaultScoringStrategy()

    @Test
    @DisplayName("조회 점수는 0.1")
    fun viewScore() {
        assertEquals(0.1, strategy.viewScore())
    }

    @Test
    @DisplayName("좋아요 점수는 0.2")
    fun likeScore() {
        assertEquals(0.2, strategy.likeScore())
    }

    @Test
    @DisplayName("주문 단위 점수는 0.7")
    fun orderScorePerUnit() {
        assertEquals(0.7, strategy.orderScorePerUnit(), 1e-6)
    }

    @Test
    @DisplayName("주문 1건(0.7)은 좋아요 3건(0.6)보다 크다")
    fun orderBeatsLikes() {
        val orderScore = 1 * strategy.orderScorePerUnit()
        val likeScore = 3 * strategy.likeScore()
        assert(orderScore > likeScore) { "order=$orderScore should > like=$likeScore" }
    }
}
