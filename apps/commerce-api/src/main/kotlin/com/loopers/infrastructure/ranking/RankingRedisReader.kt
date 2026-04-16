package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class RankedProductScore(
    val rank: Long,
    val productId: Long,
    val score: Double,
)

@Component
class RankingRedisReader(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private val KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val KEY_TTL: Duration = Duration.ofDays(2)
    }

    fun incrementScore(productId: Long, occurredAt: ZonedDateTime, score: Double) {
        val key = keyFor(occurredAt)
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        redisTemplate.expire(key, KEY_TTL)
    }

    fun getRank(date: String, productId: Long): Long? {
        return redisTemplate.opsForZSet().reverseRank(keyFor(date), productId.toString())
            ?.plus(1)
    }

    fun getRank(occurredAt: ZonedDateTime, productId: Long): Long? {
        return getRank(KEY_FORMATTER.format(occurredAt), productId)
    }

    fun getPage(date: String, start: Long, end: Long): List<RankedProductScore> {
        val entries = redisTemplate.opsForZSet().reverseRangeWithScores(keyFor(date), start, end).orEmpty()
        return entries.mapIndexedNotNull { index, tuple ->
            val productId = tuple.value?.toLongOrNull() ?: return@mapIndexedNotNull null
            RankedProductScore(
                rank = start + index + 1,
                productId = productId,
                score = tuple.score ?: 0.0,
            )
        }
    }

    fun carryOver(fromDate: String, toDate: String, limit: Long, multiplier: Double) {
        val sourceEntries = redisTemplate.opsForZSet().reverseRangeWithScores(keyFor(fromDate), 0, limit - 1).orEmpty()
        val targetKey = keyFor(toDate)
        val tuples = sourceEntries.mapNotNull { entry ->
            val value = entry.value ?: return@mapNotNull null
            val score = (entry.score ?: 0.0) * multiplier
            if (score <= 0.0) {
                return@mapNotNull null
            }
            ZSetOperations.TypedTuple.of(value, score)
        }.toSet()

        if (tuples.isNotEmpty()) {
            redisTemplate.opsForZSet().add(targetKey, tuples)
            redisTemplate.expire(targetKey, KEY_TTL)
        }
    }

    private fun keyFor(date: String): String {
        return "ranking:all:$date"
    }

    private fun keyFor(occurredAt: ZonedDateTime): String {
        return keyFor(KEY_FORMATTER.format(occurredAt))
    }
}
