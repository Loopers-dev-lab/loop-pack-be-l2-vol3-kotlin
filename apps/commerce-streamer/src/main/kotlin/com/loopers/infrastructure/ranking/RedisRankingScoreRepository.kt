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

    override fun incrementScore(productId: Long, score: Double) {
        val key = rankingKey()
        redisTemplate.execute(
            INCREMENT_WITH_TTL_SCRIPT,
            listOf(key),
            score.toString(),
            productId.toString(),
            RedisRankingConstants.RANKING_TTL_SECONDS.toString(),
        )
    }

    private fun rankingKey(): String {
        val today = LocalDate.now(RedisRankingConstants.KST_ZONE)
        return "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }

    companion object {
        private val INCREMENT_WITH_TTL_SCRIPT = DefaultRedisScript<Long>(
            """
            redis.call('ZINCRBY', KEYS[1], ARGV[1], ARGV[2])
            if redis.call('TTL', KEYS[1]) == -1 then
                redis.call('EXPIRE', KEYS[1], ARGV[3])
            end
            return 0
            """.trimIndent(),
            Long::class.javaObjectType,
        )
    }
}
