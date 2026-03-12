package com.loopers.infrastructure.ratelimit

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ratelimit.RateLimit
import com.loopers.infrastructure.cache.CacheKeyResolver
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * @RateLimit 어노테이션 처리 AOP Aspect.
 *
 * - @Transactional보다 먼저 실행 (Order.HIGHEST_PRECEDENCE)
 *   → 중복 요청 시 트랜잭션 자체가 시작되지 않음
 * - Redis SETNX + TTL로 동일 키 중복 차단
 * - Redis 장애 시 graceful degradation (통과 처리)
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitAspect(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<*, *>,
) {

    companion object {
        private val log = LoggerFactory.getLogger(RateLimitAspect::class.java)
        private const val KEY_PREFIX = "ratelimit"
    }

    @Around("@annotation(rateLimit)")
    fun around(joinPoint: ProceedingJoinPoint, rateLimit: RateLimit): Any? {
        val resolvedKey = "$KEY_PREFIX:${CacheKeyResolver.resolve(rateLimit.key, joinPoint)}"

        if (isDuplicate(resolvedKey, rateLimit.ttl)) {
            log.debug("중복 요청 무시 [key={}]", resolvedKey)
            return null
        }

        return joinPoint.proceed()
    }

    private fun isDuplicate(key: String, ttlSeconds: Long): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            val ops = (redisTemplate as RedisTemplate<String, String>).opsForValue()
            val isNew = ops.setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds))
            isNew != true
        } catch (e: Exception) {
            log.warn("Redis 장애로 RateLimit 확인 불가, 통과 처리 [key={}]", key, e)
            false
        }
    }

}
