package com.loopers.domain.queue.waiting

import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import java.util.TreeMap

class FakeWaitingQueueRepository : WaitingQueueRepository {

    private val scoreByUserId = mutableMapOf<Long, Double>()
    private val sortedEntries = TreeMap<Double, MutableList<Long>>()

    override fun enter(userId: Long, score: Double, maxCapacity: Int): Long? {
        if (userId in scoreByUserId) {
            return findPosition(userId)
        }
        if (scoreByUserId.size >= maxCapacity) {
            return null
        }
        scoreByUserId[userId] = score
        sortedEntries.getOrPut(score) { mutableListOf() }.add(userId)
        return findPosition(userId)
    }

    override fun findPosition(userId: Long): Long? {
        val userScore = scoreByUserId[userId] ?: return null
        var rank = 0L
        for ((score, userIds) in sortedEntries) {
            for (uid in userIds) {
                if (uid == userId) return rank
                rank++
            }
            if (score > userScore) break
        }
        return rank
    }

    override fun count(): Long = scoreByUserId.size.toLong()

    override fun popMin(count: Int): List<Long> {
        val result = mutableListOf<Long>()
        repeat(count) {
            val firstEntry = sortedEntries.firstEntry() ?: return result
            val userIds = firstEntry.value
            val userId = userIds.removeFirst()
            if (userIds.isEmpty()) {
                sortedEntries.pollFirstEntry()
            }
            scoreByUserId.remove(userId)
            result.add(userId)
        }
        return result
    }
}
