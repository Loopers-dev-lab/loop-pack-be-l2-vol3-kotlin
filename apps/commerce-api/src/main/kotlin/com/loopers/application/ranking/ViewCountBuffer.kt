package com.loopers.application.ranking

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class ViewCountBuffer {

    private val buffer = ConcurrentHashMap<Long, AtomicLong>()

    fun increment(productId: Long) {
        buffer.computeIfAbsent(productId) { AtomicLong(0) }.incrementAndGet()
    }

    fun drainAll(): Map<Long, Long> {
        val snapshot = mutableMapOf<Long, Long>()
        val iterator = buffer.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val count = entry.value.getAndSet(0)
            if (count > 0) {
                snapshot[entry.key] = count
            }
            if (entry.value.get() == 0L) {
                iterator.remove()
            }
        }
        return snapshot
    }
}
