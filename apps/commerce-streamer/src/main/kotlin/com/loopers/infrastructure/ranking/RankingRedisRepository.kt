package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class RankingRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {

    fun incrementScore(productId: Long, score: Double, date: LocalDate) {
        val key = buildDailyKey(date)
        masterRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        masterRedisTemplate.expire(key, KEY_TTL)
    }

    fun incrementHourlyScore(productId: Long, score: Double, dateTime: LocalDateTime) {
        val key = buildHourlyKey(dateTime)
        masterRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        masterRedisTemplate.expire(key, HOURLY_KEY_TTL)
    }

    private fun buildDailyKey(date: LocalDate): String {
        return "$KEY_PREFIX${date.format(DATE_FORMAT)}"
    }

    private fun buildHourlyKey(dateTime: LocalDateTime): String {
        return "$HOURLY_KEY_PREFIX${dateTime.format(HOURLY_DATE_FORMAT)}"
    }

    fun carryOverScores(sourceDate: LocalDate, targetDate: LocalDate, weight: Double) {
        val sourceKey = buildDailyKey(sourceDate)
        val targetKey = buildDailyKey(targetDate)

        masterRedisTemplate.execute(
            CARRY_OVER_SCRIPT,
            listOf(sourceKey, targetKey),
            weight.toString(),
            KEY_TTL.toSeconds().toString(),
        )
    }

    companion object {
        const val KEY_PREFIX = "ranking:all:"
        const val HOURLY_KEY_PREFIX = "ranking:hourly:"
        val KEY_TTL: Duration = Duration.ofDays(2)
        val HOURLY_KEY_TTL: Duration = Duration.ofHours(3)
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private val HOURLY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH")

        private val CARRY_OVER_SCRIPT = org.springframework.data.redis.core.script.RedisScript.of<Long>(
            """
            local sourceKey = KEYS[1]
            local targetKey = KEYS[2]
            local weight = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])

            local members = redis.call('ZRANGEBYSCORE', sourceKey, '-inf', '+inf', 'WITHSCORES')
            for i = 1, #members, 2 do
                local member = members[i]
                local score = tonumber(members[i + 1]) * weight
                redis.call('ZADD', targetKey, 'NX', score, member)
            end

            redis.call('EXPIRE', targetKey, ttl)
            return 1
            """.trimIndent(),
            Long::class.java,
        )
    }
}
