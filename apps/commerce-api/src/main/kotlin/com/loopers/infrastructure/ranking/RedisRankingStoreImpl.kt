package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.RankingStore
import com.loopers.domain.ranking.RankingEntry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisRankingStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingStore {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getTopProducts(key: String, offset: Long, count: Long): List<RankingEntry> {
        return try {
            val end = offset + count - 1
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, offset, end)
                ?.mapNotNull { tuple ->
                    val productId = tuple.value?.toLongOrNull() ?: return@mapNotNull null
                    val score = tuple.score ?: 0.0
                    RankingEntry(productId = productId, score = score)
                } ?: emptyList()
        } catch (e: Exception) {
            log.warn("랭킹 Top-N 조회 실패: key={}, error={}", key, e.message)
            emptyList()
        }
    }

    override fun getTotalCount(key: String): Long {
        return try {
            redisTemplate.opsForZSet().size(key) ?: 0L
        } catch (e: Exception) {
            log.warn("랭킹 총 수 조회 실패: key={}, error={}", key, e.message)
            0L
        }
    }

    override fun getRank(key: String, productId: Long): Long? {
        return try {
            redisTemplate.opsForZSet().reverseRank(key, productId.toString())
        } catch (e: Exception) {
            log.warn("랭킹 순위 조회 실패: key={}, productId={}, error={}", key, productId, e.message)
            null
        }
    }

    override fun getScore(key: String, productId: Long): Double? {
        return try {
            redisTemplate.opsForZSet().score(key, productId.toString())
        } catch (e: Exception) {
            log.warn("랭킹 점수 조회 실패: key={}, productId={}, error={}", key, productId, e.message)
            null
        }
    }
}
