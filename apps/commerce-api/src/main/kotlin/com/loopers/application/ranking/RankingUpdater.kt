package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisReader
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class RankingUpdater(
    private val rankingScoreCalculator: RankingScoreCalculator,
    private val rankingRedisReader: RankingRedisReader,
) {
    fun applyViewed(productId: Long, occurredAt: ZonedDateTime) {
        rankingRedisReader.incrementScore(productId, occurredAt, rankingScoreCalculator.viewed())
    }

    fun applyLikeChanged(productId: Long, delta: Long, occurredAt: ZonedDateTime) {
        rankingRedisReader.incrementScore(productId, occurredAt, rankingScoreCalculator.likeChanged(delta))
    }

    fun applyOrdered(productId: Long, quantity: Long, occurredAt: ZonedDateTime) {
        rankingRedisReader.incrementScore(productId, occurredAt, rankingScoreCalculator.ordered(quantity))
    }
}
