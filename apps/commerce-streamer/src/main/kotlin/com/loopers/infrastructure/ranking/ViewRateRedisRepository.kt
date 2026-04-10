package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ViewRateRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {

    fun incrementAndGetRequestCount(identifier: String, dateTime: LocalDateTime): Long {
        val key = "$REQUEST_RATE_PREFIX${dateTime.format(MINUTE_FORMAT)}:$identifier"
        val count = masterRedisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            masterRedisTemplate.expire(key, Duration.ofMinutes(2))
        }
        return count
    }

    fun addViewedProductAndGetCount(identifier: String, productId: Long, dateTime: LocalDateTime): Long {
        val key = "$DIVERSITY_PREFIX${dateTime.format(TEN_MINUTE_FORMAT)}:$identifier"
        masterRedisTemplate.opsForSet().add(key, productId.toString())
        masterRedisTemplate.expire(key, Duration.ofMinutes(15))
        return masterRedisTemplate.opsForSet().size(key) ?: 1L
    }

    companion object {
        const val REQUEST_RATE_PREFIX = "ranking:view:rate:"
        const val DIVERSITY_PREFIX = "ranking:view:diversity:"
        private val MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        private val TEN_MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH")
    }
}
