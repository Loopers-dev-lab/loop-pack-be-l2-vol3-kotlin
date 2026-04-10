package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RankingRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    companion object {
        private val TTL = Duration.ofDays(2)
    }

    override fun incrementScore(key: String, productId: Long, score: Double) {
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        if (redisTemplate.getExpire(key) == -1L) {
            redisTemplate.expire(key, TTL)
        }
    }
}
