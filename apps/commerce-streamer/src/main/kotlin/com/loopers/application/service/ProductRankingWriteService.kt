package com.loopers.application.service

import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.ProductViewedEvent
import com.loopers.domain.ranking.ProductRankingWriteRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class ProductRankingWriteService(
    private val productRankingWriteRepository: ProductRankingWriteRepository,
) {

    companion object {
        private val KST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private const val VIEW_SCORE = 0.1
        private const val LIKE_SCORE = 0.2
        private const val ORDER_SCORE_PER_QUANTITY = 0.7
    }

    fun write(event: Any) {
        val processingDate = LocalDate.now(KST_ZONE_ID)

        when (event) {
            is ProductViewedEvent -> {
                productRankingWriteRepository.incrementScore(processingDate, event.productId, VIEW_SCORE)
            }

            is LikeCountEvent -> {
                val score = when (event.type) {
                    LikeCountEventType.INCREMENT -> LIKE_SCORE
                    LikeCountEventType.DECREMENT -> -LIKE_SCORE
                }
                productRankingWriteRepository.incrementScore(processingDate, event.productId, score)
            }

            is OrderCreatedEvent -> {
                event.lineItems.forEach { lineItem ->
                    val score = lineItem.quantity * ORDER_SCORE_PER_QUANTITY
                    productRankingWriteRepository.incrementScore(processingDate, lineItem.productId, score)
                }
            }
        }
    }
}
