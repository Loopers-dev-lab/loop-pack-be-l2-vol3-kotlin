package com.loopers.domain.ranking

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import kotlin.math.log10

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
        val totalAmount = unitPrice * quantity
        if (totalAmount <= 0) {
            log.warn("주문 랭킹 점수 계산 스킵: productId={}, unitPrice={}, quantity={}", productId, unitPrice, quantity)
            return
        }
        val score = ORDER_WEIGHT * log10(totalAmount.toDouble())
        rankingRepository.incrementScore(date, productId, score)
    }
}
