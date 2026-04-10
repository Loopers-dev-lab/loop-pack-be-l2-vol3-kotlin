package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RankingRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    override fun getTopN(key: String, offset: Long, count: Long): List<RankingEntry> {
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, offset, offset + count - 1)
            ?.mapNotNull { tuple ->
                val productId = tuple.value?.toLongOrNull() ?: return@mapNotNull null
                val score = tuple.score ?: return@mapNotNull null
                RankingEntry(productId, score)
            } ?: emptyList()
    }

    override fun getRank(key: String, productId: Long): Long? {
        return redisTemplate.opsForZSet()
            .reverseRank(key, productId.toString())
    }

    override fun getScore(key: String, productId: Long): Double? {
        return redisTemplate.opsForZSet()
            .score(key, productId.toString())
    }

    override fun getTotalCount(key: String): Long {
        return redisTemplate.opsForZSet().zCard(key) ?: 0
    }
}
