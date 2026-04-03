package com.loopers.domain.queue.fixture

import com.loopers.domain.queue.WaitingQueueRepository
import java.util.TreeMap

class FakeWaitingQueueRepository : WaitingQueueRepository {

    private val queue = TreeMap<Double, Long>()
    private val memberScores = mutableMapOf<Long, Double>()

    override fun enqueue(userId: Long, score: Double): Boolean {
        if (memberScores.containsKey(userId)) return false
        memberScores[userId] = score
        queue[score] = userId
        return true
    }

    override fun getPosition(userId: Long): Long? {
        val score = memberScores[userId] ?: return null
        return queue.headMap(score).size.toLong()
    }

    override fun getQueueSize(): Long = queue.size.toLong()

    override fun dequeueTopN(count: Long): List<Long> {
        val result = mutableListOf<Long>()
        repeat(count.toInt().coerceAtMost(queue.size)) {
            val entry = queue.pollFirstEntry() ?: return@repeat
            memberScores.remove(entry.value)
            result.add(entry.value)
        }
        return result
    }

    override fun remove(userId: Long) {
        val score = memberScores.remove(userId) ?: return
        queue.remove(score)
    }
}
