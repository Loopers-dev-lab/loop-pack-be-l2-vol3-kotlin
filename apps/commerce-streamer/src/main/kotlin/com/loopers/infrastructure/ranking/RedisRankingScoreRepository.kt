package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisRankingConstants
import com.loopers.domain.ranking.repository.RankingScoreRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class RedisRankingScoreRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingScoreRepository {

    override fun incrementScore(productId: Long, score: Double, eventId: String, rankingDate: LocalDate) {
        val key = rankingKey(rankingDate)
        val processedKey = processedKey(rankingDate)
        redisTemplate.execute(
            IDEMPOTENT_INCREMENT_SCRIPT,
            listOf(key, processedKey),
            score.toString(),
            productId.toString(),
            RedisRankingConstants.RANKING_TTL_SECONDS.toString(),
            eventId,
        )
    }

    private fun rankingKey(date: LocalDate): String {
        return "${RedisRankingConstants.RANKING_KEY_PREFIX}${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }

    private fun processedKey(date: LocalDate): String {
        return "${PROCESSED_KEY_PREFIX}${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }

    companion object {
        private const val PROCESSED_KEY_PREFIX = "ranking:processed:"

        private val IDEMPOTENT_INCREMENT_SCRIPT = DefaultRedisScript<Long>(
            """
            if redis.call('SISMEMBER', KEYS[2], ARGV[4]) == 1 then
                return 0
            end
            redis.call('SADD', KEYS[2], ARGV[4])
            redis.call('ZINCRBY', KEYS[1], ARGV[1], ARGV[2])
            if redis.call('TTL', KEYS[1]) == -1 then
                redis.call('EXPIRE', KEYS[1], ARGV[3])
            end
            if redis.call('TTL', KEYS[2]) == -1 then
                redis.call('EXPIRE', KEYS[2], ARGV[3])
            end
            return 1
            """.trimIndent(),
            Long::class.javaObjectType,
        )
    }
}
