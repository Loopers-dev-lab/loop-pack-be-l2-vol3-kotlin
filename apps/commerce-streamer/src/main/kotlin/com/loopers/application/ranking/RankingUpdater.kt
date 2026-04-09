package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisStore
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class RankingUpdater(
    private val rankingScoreCalculator: RankingScoreCalculator,
    private val rankingRedisStore: RankingRedisStore,
) {
    fun applyViewed(productId: Long, occurredAt: ZonedDateTime) {
        rankingRedisStore.incrementScore(productId, occurredAt, rankingScoreCalculator.viewed())
    }

    fun applyLikeChanged(productId: Long, delta: Long, occurredAt: ZonedDateTime) {
        rankingRedisStore.incrementScore(productId, occurredAt, rankingScoreCalculator.likeChanged(delta))
    }

    fun applyOrdered(productId: Long, quantity: Long, occurredAt: ZonedDateTime) {
        rankingRedisStore.incrementScore(productId, occurredAt, rankingScoreCalculator.ordered(quantity))
    }
}
