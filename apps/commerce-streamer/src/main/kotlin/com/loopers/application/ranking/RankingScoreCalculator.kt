package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingScoreFormula
import org.springframework.stereotype.Component

@Component
class RankingScoreCalculator {
    fun viewed(): Double = RankingScoreFormula.VIEW_WEIGHT

    fun likeChanged(delta: Long): Double = RankingScoreFormula.LIKE_WEIGHT * delta

    fun ordered(quantity: Long): Double = RankingScoreFormula.SALES_WEIGHT * quantity
}
