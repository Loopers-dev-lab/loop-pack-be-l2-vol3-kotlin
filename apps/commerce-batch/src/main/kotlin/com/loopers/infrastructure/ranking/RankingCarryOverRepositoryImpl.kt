package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingCarryOverRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RankingCarryOverRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) : RankingCarryOverRepository {

    companion object {
        private val TTL = Duration.ofDays(2)
    }

    override fun carryOver(sourceKey: String, destKey: String, carryOverWeight: Double): Long {
        val members = redisTemplate.opsForZSet()
            .reverseRangeWithScores(sourceKey, 0, -1) ?: return 0

        if (members.isEmpty()) return 0

        members.forEach { tuple ->
            val member = tuple.value ?: return@forEach
            val score = tuple.score ?: return@forEach
            redisTemplate.opsForZSet().incrementScore(destKey, member, score * carryOverWeight)
        }

        if (redisTemplate.getExpire(destKey) == -1L) {
            redisTemplate.expire(destKey, TTL)
        }

        return members.size.toLong()
    }
}
