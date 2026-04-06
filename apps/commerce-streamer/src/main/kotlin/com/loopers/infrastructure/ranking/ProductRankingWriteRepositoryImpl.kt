package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingWriteRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ProductRankingWriteRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : ProductRankingWriteRepository {

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private val TTL: Duration = Duration.ofDays(2)

        // TTL이 없을 때만 원자적으로 EXPIRE를 설정하는 Lua script (Redis 7.0 미만 호환)
        private val setTtlIfAbsentScript = RedisScript.of(
            """
            local ttl = redis.call('TTL', KEYS[1])
            if ttl < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return ttl
            """.trimIndent(),
            Long::class.java,
        )
    }

    override fun incrementScore(processingDate: LocalDate, productId: Long, score: Double) {
        val key = key(processingDate)
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        redisTemplate.execute(setTtlIfAbsentScript, listOf(key), TTL.seconds.toString())
    }

    private fun key(processingDate: LocalDate): String = "$KEY_PREFIX${processingDate.format(DATE_FORMATTER)}"
}
