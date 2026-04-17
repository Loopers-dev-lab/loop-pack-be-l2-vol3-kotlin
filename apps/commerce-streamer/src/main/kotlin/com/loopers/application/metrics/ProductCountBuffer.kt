package com.loopers.application.metrics

import com.loopers.hash.MetricType
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

@Component
class ProductCountBuffer {

    private val buffer = ConcurrentHashMap<Key, LongAdder>()

    fun add(productId: Long, type: MetricType, amount: Long = 1L) {
        buffer.computeIfAbsent(Key(productId, type)) { LongAdder() }.add(amount)
    }

    fun drainAll(): Map<Key, Long> {
        val snapshot = mutableMapOf<Key, Long>()
        val iterator = buffer.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val accumulated = entry.value.sumThenReset()
            if (accumulated != 0L) {
                snapshot[entry.key] = accumulated
            }
            if (entry.value.sum() == 0L) {
                iterator.remove()
            }
        }
        return snapshot
    }

    data class Key(val productId: Long, val type: MetricType)
}
