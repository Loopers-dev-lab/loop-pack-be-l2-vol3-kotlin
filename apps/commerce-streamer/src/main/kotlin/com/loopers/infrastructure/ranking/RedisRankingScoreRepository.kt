package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.ranking.repository.RankingScoreRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Repository
class RedisRankingScoreRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingScoreRepository {

    override fun incrementScore(productId: Long, score: Double) {
        val key = rankingKey()
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        setTtlIfAbsent(key)
    }

    private fun setTtlIfAbsent(key: String) {
        val ttl = redisTemplate.getExpire(key)
        if (ttl == -1L) {
            redisTemplate.expire(key, RedisRankingConstants.RANKING_TTL_SECONDS, TimeUnit.SECONDS)
        }
    }

    private fun rankingKey(): String {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        return "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }
}
