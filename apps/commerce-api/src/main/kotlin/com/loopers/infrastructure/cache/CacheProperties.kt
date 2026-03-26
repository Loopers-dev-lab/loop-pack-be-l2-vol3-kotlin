package com.loopers.infrastructure.cache

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cache")
data class CacheProperties(
    val mode: CacheMode = CacheMode.LAYERED,
    val caffeine: CaffeineProperties = CaffeineProperties(),
    val stampede: StampedeProperties = StampedeProperties(),
) {
    data class CaffeineProperties(
        val maxSize: Long = 5000,
        val refreshSeconds: Long = 2,
        val expireSeconds: Long = 10,
    )

    data class StampedeProperties(
        val strategy: StampedeStrategy = StampedeStrategy.NONE,
    )
}

enum class CacheMode {
    DB_ONLY,
    REDIS_ONLY,
    CAFFEINE_ONLY,
    LAYERED,
}

enum class StampedeStrategy {
    NONE,
    MUTEX,
    SINGLE_FLIGHT,
    WARMUP,
}
