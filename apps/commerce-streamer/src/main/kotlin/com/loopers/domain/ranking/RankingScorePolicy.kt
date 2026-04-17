package com.loopers.domain.ranking

import com.loopers.config.kafka.event.CatalogEventType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RankingScorePolicy(
    @Value("\${ranking.weight.view:0.1}") private val viewWeight: Double,
    @Value("\${ranking.weight.like:0.2}") private val likeWeight: Double,
    @Value("\${ranking.weight.order:0.7}") private val orderWeight: Double,
) {
    fun calculateIncrement(eventType: CatalogEventType, delta: Long): Double {
        val weight = when (eventType) {
            CatalogEventType.PRODUCT_VIEWED -> viewWeight
            CatalogEventType.LIKE_CHANGED -> likeWeight
            CatalogEventType.ORDER_COMPLETED -> orderWeight
        }
        return weight * delta
    }
}
