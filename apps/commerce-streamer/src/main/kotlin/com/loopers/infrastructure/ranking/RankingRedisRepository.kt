package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RankingRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "ranking:all"
        private val TTL = Duration.ofDays(2)
    }

    fun replaceAll(date: String, scores: Map<Long, Double>) {
        if (scores.isEmpty()) return

        val key = buildKey(date)
        masterRedisTemplate.executePipelined { connection ->
            scores.forEach { (productId, score) ->
                connection.zSetCommands().zAdd(
                    key.toByteArray(),
                    score,
                    productId.toString().toByteArray(),
                )
            }
            null
        }
        masterRedisTemplate.expire(key, TTL)
        log.info("[RankingRedis] Synced {} products to key={}", scores.size, key)
    }

    private fun buildKey(date: String) = "$KEY_PREFIX:$date"
}
