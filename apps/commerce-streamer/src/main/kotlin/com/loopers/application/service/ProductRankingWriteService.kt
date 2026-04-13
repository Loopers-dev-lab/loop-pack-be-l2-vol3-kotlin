package com.loopers.application.service

import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.ProductViewedEvent
import com.loopers.domain.ranking.ProductRankingWriteRepository
import com.loopers.domain.ranking.ScoringStrategy
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class ProductRankingWriteService(
    private val productRankingWriteRepository: ProductRankingWriteRepository,
    private val scoringStrategy: ScoringStrategy,
) {

    companion object {
        private val KST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }

    fun write(event: Any) {
        val processingDate = LocalDate.now(KST_ZONE_ID)

        when (event) {
            is ProductViewedEvent -> {
                productRankingWriteRepository.incrementScore(processingDate, event.productId, scoringStrategy.viewScore())
            }

            is LikeCountEvent -> {
                val score = when (event.type) {
                    LikeCountEventType.INCREMENT -> scoringStrategy.likeScore()
                    LikeCountEventType.DECREMENT -> -scoringStrategy.likeScore()
                }
                productRankingWriteRepository.incrementScore(processingDate, event.productId, score)
            }

            is OrderCreatedEvent -> {
                event.lineItems.forEach { lineItem ->
                    val score = lineItem.quantity * scoringStrategy.orderScorePerUnit()
                    productRankingWriteRepository.incrementScore(processingDate, lineItem.productId, score)
                }
            }
        }
    }
}
