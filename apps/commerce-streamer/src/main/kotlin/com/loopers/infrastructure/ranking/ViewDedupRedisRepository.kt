package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ViewDedupOperations
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ViewDedupRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : ViewDedupOperations {

    override fun isDuplicate(productId: Long, loginId: String?, clientIp: String?, date: LocalDate): Boolean {
        val key = buildKey(productId, loginId, clientIp, date)
        return masterRedisTemplate.hasKey(key)
    }

    override fun markViewed(productId: Long, loginId: String?, clientIp: String?, date: LocalDate) {
        val key = buildKey(productId, loginId, clientIp, date)
        masterRedisTemplate.opsForValue().set(key, "1", KEY_TTL)
    }

    private fun buildKey(productId: Long, loginId: String?, clientIp: String?, date: LocalDate): String {
        val dateStr = date.format(DATE_FORMAT)
        return if (loginId != null) {
            "$KEY_PREFIX$dateStr:$productId:user:$loginId"
        } else {
            "$KEY_PREFIX$dateStr:$productId:ip:${clientIp ?: "unknown"}"
        }
    }

    companion object {
        const val KEY_PREFIX = "ranking:view:dedup:"
        val KEY_TTL: Duration = Duration.ofDays(1)
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}
