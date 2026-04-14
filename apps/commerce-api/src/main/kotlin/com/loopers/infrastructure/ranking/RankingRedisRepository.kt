package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RankingRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getTopNWithScores(key: String, start: Long, end: Long): List<RankingEntry> {
        return try {
            val tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, start, end)
                ?: return emptyList()
            tuples.mapNotNull { tuple ->
                val productId = tuple.value?.toLongOrNull() ?: return@mapNotNull null
                val score = tuple.score ?: return@mapNotNull null
                RankingEntry(productId = productId, score = score)
            }
        } catch (e: Exception) {
            log.warn("[RankingRedis] Failed to get ranking: key={}", key, e)
            emptyList()
        }
    }

    override fun getTotalCount(key: String): Long {
        return try {
            redisTemplate.opsForZSet().zCard(key) ?: 0
        } catch (e: Exception) {
            log.warn("[RankingRedis] Failed to get total count: key={}", key, e)
            0
        }
    }

    override fun getRank(key: String, productId: Long): Long? {
        return try {
            redisTemplate.opsForZSet().reverseRank(key, productId.toString())
        } catch (e: Exception) {
            log.warn("[RankingRedis] Failed to get rank: key={} productId={}", key, productId, e)
            null
        }
    }
}
