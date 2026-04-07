package com.loopers.application.service

import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.ProductViewedEvent
import com.loopers.domain.ranking.ProductRankingWriteRepository
import com.loopers.domain.ranking.RankingScoreStrategy
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class ProductRankingWriteService(
    private val productRankingWriteRepository: ProductRankingWriteRepository,
    private val scoreStrategy: RankingScoreStrategy,
) {

    companion object {
        private val KST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }

    fun write(event: Any) {
        val processingDate = LocalDate.now(KST_ZONE_ID)
        val daysAgo = 0 // 현재 이벤트는 항상 오늘 발생

        when (event) {
            is ProductViewedEvent -> {
                val score = scoreStrategy.calculateViewScore(daysAgo)
                productRankingWriteRepository.incrementScore(processingDate, event.productId, score)
            }

            is LikeCountEvent -> {
                val increment = event.type == LikeCountEventType.INCREMENT
                val score = scoreStrategy.calculateLikeScore(increment, daysAgo)
                productRankingWriteRepository.incrementScore(processingDate, event.productId, score)
            }

            is OrderCreatedEvent -> {
                event.lineItems.forEach { lineItem ->
                    val score = scoreStrategy.calculateOrderScore(lineItem.quantity, daysAgo)
                    productRankingWriteRepository.incrementScore(processingDate, lineItem.productId, score)
                }
            }
        }
    }
}
