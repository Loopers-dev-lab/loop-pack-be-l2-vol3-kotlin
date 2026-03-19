package com.loopers.infrastructure.cache

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class CacheMetrics(
    private val meterRegistry: MeterRegistry,
) {
    fun recordDetailHit(layer: String) {
        counter("cache.product.detail.hit", layer).increment()
    }

    fun recordDetailMiss(layer: String) {
        counter("cache.product.detail.miss", layer).increment()
    }

    fun recordListHit(layer: String) {
        counter("cache.product.list.hit", layer).increment()
    }

    fun recordListMiss(layer: String) {
        counter("cache.product.list.miss", layer).increment()
    }

    private fun counter(name: String, layer: String): Counter {
        return Counter.builder(name)
            .tag("layer", layer)
            .register(meterRegistry)
    }
}
