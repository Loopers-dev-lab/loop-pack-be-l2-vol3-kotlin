package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class RankingScoreUpdater(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 키별 TTL 설정 여부를 추적하여 매번 expire 호출을 방지한다.
    // 키가 날짜 기반이므로 엔트리 수는 최대 2~3개 수준이다.
    private val ttlInitialized = ConcurrentHashMap<String, Boolean>()

    fun incrementScore(productId: Long, eventType: RankingEventType, eventDate: LocalDate, score: Double = 1.0) {
        val key = buildKey(eventDate)
        val delta = eventType.weight * score
        masterRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), delta)
        ensureTtl(key)
    }

    fun buildKey(date: LocalDate): String {
        return "$KEY_PREFIX${date.format(DATE_FORMATTER)}"
    }

    private fun ensureTtl(key: String) {
        ttlInitialized.computeIfAbsent(key) {
            masterRedisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS)
            true
        }
    }

    companion object {
        const val KEY_PREFIX = "ranking:all:"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val TTL_DAYS = 2L
    }
}

enum class RankingEventType(val weight: Double) {
    VIEW(0.1),
    LIKE(0.2),
    ORDER(0.7),
}
