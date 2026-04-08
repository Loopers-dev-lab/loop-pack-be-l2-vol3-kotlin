package com.loopers.domain.ranking

import org.springframework.stereotype.Component
import java.time.LocalDate
import kotlin.math.log10

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {
    companion object {
        private const val VIEW_WEIGHT = 0.1
        private const val LIKE_WEIGHT = 0.2
        private const val ORDER_WEIGHT = 0.7
    }

    fun updateScoreForView(date: LocalDate, productId: Long) {
        rankingRepository.incrementScore(date, productId, VIEW_WEIGHT)
    }

    fun updateScoreForLike(date: LocalDate, productId: Long) {
        rankingRepository.incrementScore(date, productId, LIKE_WEIGHT)
    }

    fun updateScoreForUnlike(date: LocalDate, productId: Long) {
        rankingRepository.incrementScore(date, productId, -LIKE_WEIGHT)
    }

    fun updateScoreForOrder(date: LocalDate, productId: Long, unitPrice: Long, quantity: Int) {
        val score = ORDER_WEIGHT * log10((unitPrice * quantity).toDouble())
        rankingRepository.incrementScore(date, productId, score)
    }
}
