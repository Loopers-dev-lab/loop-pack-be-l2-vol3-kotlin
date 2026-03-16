package com.loopers.config.redis

import org.springframework.data.redis.cache.RedisCacheWriter
import java.time.Duration
import kotlin.random.Random

/**
 * TTL에 무작위 지터를 추가하여 Cache Stampede를 방지하는 RedisCacheWriter 래퍼.
 * 동일 TTL 캐시가 동시에 만료되는 thundering herd 문제를 완화한다.
 */
class JitteredRedisCacheWriter(
    private val delegate: RedisCacheWriter,
    private val maxJitterSeconds: Long,
) : RedisCacheWriter by delegate {

    override fun put(name: String, key: ByteArray, value: ByteArray, ttl: Duration?) {
        delegate.put(name, key, value, ttl?.withJitter())
    }

    override fun putIfAbsent(name: String, key: ByteArray, value: ByteArray, ttl: Duration?): ByteArray? {
        return delegate.putIfAbsent(name, key, value, ttl?.withJitter())
    }

    private fun Duration.withJitter(): Duration =
        plusSeconds(Random.nextLong(-maxJitterSeconds, maxJitterSeconds + 1))
            .let { if (it.isNegative || it.isZero) Duration.ofSeconds(1) else it }
}
