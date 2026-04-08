package com.loopers.application.ranking

import org.springframework.stereotype.Component

@Component
class RankingScoreCalculator {
    companion object {
        private const val VIEW_SCORE = 0.1
        private const val LIKE_SCORE = 0.2
    }

    fun viewed(): Double = VIEW_SCORE

    fun likeChanged(delta: Long): Double = LIKE_SCORE * delta

    fun ordered(quantity: Long): Double = quantity.toDouble()
}
