package com.loopers.infrastructure.config

import org.springframework.boot.autoconfigure.cache.CacheProperties
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import java.time.Duration
import kotlin.random.Random

@Configuration
class CacheConfig {
    companion object {
        private const val JITTER_PERCENTAGE = 0.1 // ±10%
    }

    @Bean
    fun redisCacheManagerBuilderCustomizer(cacheProperties: CacheProperties): RedisCacheManagerBuilderCustomizer {
        return RedisCacheManagerBuilderCustomizer { builder ->
            val baseTtl = cacheProperties.redis.timeToLive ?: Duration.ofMinutes(30)
            val jitteredTtl = applyJitter(baseTtl)

            val config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(jitteredTtl)

            builder.cacheDefaults(config)
        }
    }

    private fun applyJitter(baseDuration: Duration): Duration {
        val millis = baseDuration.toMillis()
        val jitterAmount = (millis * JITTER_PERCENTAGE).toLong()
        val randomJitter = Random.nextLong(-jitterAmount, jitterAmount + 1)
        val jitteredMillis = maxOf(millis + randomJitter, 1L)
        return Duration.ofMillis(jitteredMillis)
    }
}
