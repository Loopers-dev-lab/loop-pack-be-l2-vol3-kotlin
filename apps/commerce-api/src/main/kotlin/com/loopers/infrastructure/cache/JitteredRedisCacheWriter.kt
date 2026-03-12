package com.loopers.infrastructure.cache

import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration
import kotlin.random.Random

/**
 * 엔트리별 TTL Jitter를 적용하는 RedisCacheWriter 래퍼.
 *
 * 동일 TTL 캐시가 동시에 만료되는 Cache Stampede를 방지한다.
 * - 예: base TTL 10분 + jitter 60초 → 실제 TTL은 10분 0초 ~ 11분 0초 사이 랜덤
 *
 * [CacheName.l2JitterSeconds]에서 cacheName별 jitter 범위를 읽는다.
 * cacheName이 매핑에 없으면 jitter 없이 원본 TTL 그대로 적용.
 */
class JitteredRedisCacheWriter(
    connectionFactory: RedisConnectionFactory,
    private val jitterSecondsMap: Map<String, Long>,
) : RedisCacheWriter by RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory) {

    private val delegate = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)

    override fun put(name: String, key: ByteArray, value: ByteArray, ttl: Duration?) {
        val jitteredTtl = applyJitter(name, ttl)
        delegate.put(name, key, value, jitteredTtl)
    }

    override fun putIfAbsent(name: String, key: ByteArray, value: ByteArray, ttl: Duration?): ByteArray? {
        val jitteredTtl = applyJitter(name, ttl)
        return delegate.putIfAbsent(name, key, value, jitteredTtl)
    }

    private fun applyJitter(cacheName: String, ttl: Duration?): Duration? {
        if (ttl == null || ttl.isZero) return ttl
        val maxJitter = jitterSecondsMap[cacheName] ?: return ttl
        if (maxJitter <= 0) return ttl
        val jitter = Random.nextLong(0, maxJitter)
        return ttl.plusSeconds(jitter)
    }
}
