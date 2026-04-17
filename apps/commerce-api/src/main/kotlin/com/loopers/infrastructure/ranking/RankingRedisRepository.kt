package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingScore
import com.loopers.domain.ranking.RankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    override fun getTopRankings(date: LocalDate, start: Long, end: Long): List<ProductRankingScore> {
        val key = buildKey(date)
        val tuples = masterRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end)
            ?: return emptyList()

        return tuples.mapNotNull { tuple ->
            val productId = tuple.value?.toLongOrNull() ?: return@mapNotNull null
            val score = tuple.score ?: return@mapNotNull null
            ProductRankingScore(productId = productId, score = score)
        }
    }

    override fun getProductRank(productId: Long, date: LocalDate): Long? {
        val key = buildKey(date)
        return masterRedisTemplate.opsForZSet().reverseRank(key, productId.toString())
    }

    override fun getProductScore(productId: Long, date: LocalDate): Double? {
        val key = buildKey(date)
        return masterRedisTemplate.opsForZSet().score(key, productId.toString())
    }

    override fun getTotalCount(date: LocalDate): Long {
        val key = buildKey(date)
        return masterRedisTemplate.opsForZSet().zCard(key) ?: 0
    }

    private fun buildKey(date: LocalDate): String {
        return "$KEY_PREFIX${date.format(DATE_FORMAT)}"
    }

    companion object {
        const val KEY_PREFIX = "ranking:all:"
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}
