package com.loopers.application.ranking

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.DoubleAdder

@Component
class RankingScoreBuffer {

    private val buffer = ConcurrentHashMap<Long, DoubleAdder>()

    fun add(productId: Long, score: Double) {
        buffer.computeIfAbsent(productId) { DoubleAdder() }.add(score)
    }

    fun drainAll(): Map<Long, Double> {
        val snapshot = mutableMapOf<Long, Double>()
        val iterator = buffer.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val score = entry.value.sumThenReset()
            if (score != 0.0) {
                snapshot[entry.key] = score
            }
            if (entry.value.sum() == 0.0) {
                iterator.remove()
            }
        }
        return snapshot
    }
}
