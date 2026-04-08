package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.RankingEntry
import com.loopers.application.ranking.RankingStore
import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingRedisStore(
    private val redisZSetTemplate: RedisZSetTemplate,
) : RankingStore {

    override fun getTopRankings(date: LocalDate, offset: Long, limit: Long): List<RankingEntry> {
        val key = RankingKeyGenerator.dailyKey(date)
        return redisZSetTemplate.reverseRangeWithScores(key, offset, offset + limit - 1)
            .map { entry -> RankingEntry(entry.member.toLong(), entry.score) }
    }

    override fun getProductRank(date: LocalDate, productId: Long): Long? {
        val key = RankingKeyGenerator.dailyKey(date)
        return redisZSetTemplate.reverseRank(key, productId.toString())
    }

    override fun getProductScore(date: LocalDate, productId: Long): Double? {
        val key = RankingKeyGenerator.dailyKey(date)
        return redisZSetTemplate.score(key, productId.toString())
    }

    override fun size(date: LocalDate): Long {
        val key = RankingKeyGenerator.dailyKey(date)
        return redisZSetTemplate.size(key)
    }
}
