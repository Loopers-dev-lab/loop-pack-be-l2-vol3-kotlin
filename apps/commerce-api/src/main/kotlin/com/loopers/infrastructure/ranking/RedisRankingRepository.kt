package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisRankingRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    override fun getTopRankings(date: String, offset: Long, size: Long): List<RankingEntry> {
        val key = buildKey(date)
        val start = offset
        val end = offset + size - 1
        val tuples = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, start, end) ?: return emptyList()

        return tuples.mapIndexedNotNull { index, tuple ->
            val productId = tuple.value?.toLongOrNull() ?: return@mapIndexedNotNull null
            RankingEntry(
                productId = productId,
                score = tuple.score ?: 0.0,
                rank = offset + index,
            )
        }
    }

    override fun getRank(date: String, productId: Long): Long? {
        val key = buildKey(date)
        return redisTemplate.opsForZSet().reverseRank(key, productId.toString())
    }

    override fun getScore(date: String, productId: Long): Double? {
        val key = buildKey(date)
        return redisTemplate.opsForZSet().score(key, productId.toString())
    }

    override fun getTotalCount(date: String): Long {
        val key = buildKey(date)
        return redisTemplate.opsForZSet().zCard(key) ?: 0L
    }

    private fun buildKey(date: String): String = "$KEY_PREFIX$date"

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
    }
}
