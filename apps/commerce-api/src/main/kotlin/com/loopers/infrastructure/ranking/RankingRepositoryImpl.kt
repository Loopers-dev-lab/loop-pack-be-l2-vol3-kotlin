package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.RankingWindow
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

/**
 * 랭킹 ZSET 읽기 구현체.
 *
 * 기본 RedisTemplate(REPLICA_PREFERRED)을 사용하여 읽기 부하를 분산한다.
 * 일간/시간 윈도우 모두 같은 ZSET 연산으로 처리 가능하다 (키만 다름).
 */
@Repository
class RankingRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    override fun getTopRankings(
        window: RankingWindow,
        windowKey: String,
        offset: Long,
        limit: Long,
    ): List<RankingEntry> {
        val key = resolveKey(window, windowKey)
        val end = offset + limit - 1

        val typedTuples = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, offset, end)
            ?: return emptyList()

        return typedTuples.mapNotNull { tuple ->
            val productId = tuple.value?.toLongOrNull() ?: return@mapNotNull null
            val score = tuple.score ?: return@mapNotNull null
            RankingEntry(productId = productId, score = score)
        }
    }

    override fun getRank(window: RankingWindow, windowKey: String, productId: Long): Long? {
        val key = resolveKey(window, windowKey)
        return redisTemplate.opsForZSet().reverseRank(key, productId.toString())
    }

    override fun getScore(window: RankingWindow, windowKey: String, productId: Long): Double? {
        val key = resolveKey(window, windowKey)
        return redisTemplate.opsForZSet().score(key, productId.toString())
    }

    override fun getTotalCount(window: RankingWindow, windowKey: String): Long {
        val key = resolveKey(window, windowKey)
        return redisTemplate.opsForZSet().size(key) ?: 0L
    }

    private fun resolveKey(window: RankingWindow, windowKey: String): String {
        return when (window) {
            RankingWindow.DAILY -> RankingRedisKeys.dailyKey(windowKey)
            RankingWindow.HOURLY -> RankingRedisKeys.hourlyKey(windowKey)
            RankingWindow.WEEKLY, RankingWindow.MONTHLY ->
                throw IllegalArgumentException("Redis는 DAILY/HOURLY만 지원합니다: $window")
        }
    }
}
