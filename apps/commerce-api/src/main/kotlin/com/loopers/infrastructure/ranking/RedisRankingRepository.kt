package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisRankingConstants
import com.loopers.domain.ranking.model.RankingEntry
import com.loopers.domain.ranking.repository.RankingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class RedisRankingRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getTopN(date: LocalDate, offset: Int, limit: Int): List<RankingEntry> {
        val key = rankingKey(date)
        val results = redisTemplate.opsForZSet()
            .reverseRangeByScoreWithScores(key, Double.MIN_VALUE, Double.MAX_VALUE, offset.toLong(), limit.toLong())
            ?: return emptyList()
        return results.mapNotNull { tuple ->
            val productId = tuple.value?.toLongOrNull() ?: run {
                log.warn("Redis 랭킹 파싱 실패 [key={}, member={}]", key, tuple.value)
                return@mapNotNull null
            }
            val score = tuple.score ?: return@mapNotNull null
            RankingEntry(productId = productId, score = score)
        }
    }

    override fun getRank(date: LocalDate, productId: Long): Int? {
        val key = rankingKey(date)
        return try {
            val rank = redisTemplate.opsForZSet().reverseRank(key, productId.toString())
                ?: return null
            val score = redisTemplate.opsForZSet().score(key, productId.toString())
                ?: return null
            if (score <= 0) return null
            (rank + 1).toInt()
        } catch (e: Exception) {
            log.warn("Redis 순위 조회 실패 [key={}, productId={}]: {}", key, productId, e.message)
            null
        }
    }

    private fun rankingKey(date: LocalDate): String =
        "${RedisRankingConstants.RANKING_KEY_PREFIX}${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
}
