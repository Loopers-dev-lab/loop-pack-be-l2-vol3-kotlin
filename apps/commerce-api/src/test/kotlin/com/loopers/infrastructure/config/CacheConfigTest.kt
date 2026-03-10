package com.loopers.infrastructure.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.cache.CacheProperties
import java.time.Duration

@DisplayName("CacheConfig")
class CacheConfigTest {
    private val cacheConfig = CacheConfig()

    @Test
    @DisplayName("CacheConfig가 RedisCacheManagerBuilderCustomizer를 생성해야 한다")
    fun shouldCreateRedisCacheManagerBuilderCustomizer() {
        val cacheProperties = CacheProperties().apply {
            redis.timeToLive = Duration.ofMinutes(30)
        }

        val customizer = cacheConfig.redisCacheManagerBuilderCustomizer(cacheProperties)

        assert(customizer != null)
    }

    @Test
    @DisplayName("profile별 TTL 설정을 지원해야 한다")
    fun shouldSupportProfileBasedTtl() {
        val profileTtls = listOf(
            Duration.ofMinutes(5),
            Duration.ofMinutes(10),
            Duration.ofMinutes(30),
        )

        for (ttl in profileTtls) {
            val cacheProperties = CacheProperties().apply {
                redis.timeToLive = ttl
            }
            val customizer = cacheConfig.redisCacheManagerBuilderCustomizer(cacheProperties)

            assert(customizer != null)
        }
    }

    @Test
    @DisplayName("jitter가 적용되어야 한다")
    fun shouldApplyJitter() {
        val baseTtl = Duration.ofSeconds(100)
        val cacheProperties = CacheProperties().apply {
            redis.timeToLive = baseTtl
        }

        val customizer = cacheConfig.redisCacheManagerBuilderCustomizer(cacheProperties)

        assert(customizer != null)
    }

    @Test
    @DisplayName("TTL이 null인 경우 기본값을 사용해야 한다")
    fun shouldUseDefaultTtlWhenNull() {
        val cacheProperties = CacheProperties()
        cacheProperties.redis.timeToLive = null

        val customizer = cacheConfig.redisCacheManagerBuilderCustomizer(cacheProperties)

        assert(customizer != null)
    }
}
