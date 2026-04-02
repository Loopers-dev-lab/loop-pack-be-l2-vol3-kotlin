package com.loopers.domain.queue

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 인메모리 대기열 Fake.
 * Redis Sorted Set의 동작을 ConcurrentSkipListMap으로 모사한다.
 * - score 기반 정렬 (오름차순)
 * - NX: 이미 존재하면 추가하지 않음
 * - popMin: 앞에서 N개 꺼내면서 삭제
 */
class FakeQueueRepository : QueueRepository {

    /** score → userId 맵 (score 오름차순 정렬) */
    private val sortedEntries = ConcurrentSkipListMap<Double, Long>()

    /** userId → score 역인덱스 (빠른 rank 조회용) */
    private val userScoreMap = mutableMapOf<Long, Double>()

    private val scoreSequence = AtomicLong(0)

    override fun add(userId: Long): Boolean {
        if (userScoreMap.containsKey(userId)) return false

        val score = scoreSequence.incrementAndGet().toDouble()
        sortedEntries[score] = userId
        userScoreMap[userId] = score
        return true
    }

    override fun getRank(userId: Long): Long? {
        val userScore = userScoreMap[userId] ?: return null
        var rank = 0L
        for ((score, _) in sortedEntries) {
            if (score == userScore) return rank
            rank++
        }
        return null
    }

    override fun size(): Long = sortedEntries.size.toLong()

    override fun popMin(count: Long): List<QueueEntry> {
        val result = mutableListOf<QueueEntry>()
        repeat(count.toInt().coerceAtMost(sortedEntries.size)) {
            val entry = sortedEntries.pollFirstEntry() ?: return result
            userScoreMap.remove(entry.value)
            result.add(QueueEntry(userId = entry.value, score = entry.key))
        }
        return result
    }

    /** userId → claimedAtMs */
    private val processingEntries = ConcurrentHashMap<Long, Long>()

    private var enabled: Boolean = false

    override fun moveToProcessing(count: Long): List<QueueEntry> {
        val result = popMin(count)
        val claimedAt = System.currentTimeMillis()
        result.forEach { processingEntries[it.userId] = claimedAt }
        return result
    }

    override fun removeFromProcessing(userIds: List<Long>) {
        userIds.forEach { processingEntries.remove(it) }
    }

    override fun recoverExpiredToWaiting(beforeTimestampMs: Long): Int {
        val expired = processingEntries.entries
            .filter { it.value <= beforeTimestampMs }
            .map { it.key }
        if (expired.isEmpty()) return 0
        expired.forEach { userId ->
            processingEntries.remove(userId)
            // 이미 재진입하지 않은 경우에만 복구 (NX)
            if (!userScoreMap.containsKey(userId)) {
                val score = scoreSequence.incrementAndGet().toDouble()
                sortedEntries[score] = userId
                userScoreMap[userId] = score
            }
        }
        return expired.size
    }

    override fun isEnabled(): Boolean = enabled

    override fun enable() {
        enabled = true
    }

    override fun disable() {
        enabled = false
    }

    fun clear() {
        sortedEntries.clear()
        userScoreMap.clear()
        processingEntries.clear()
        scoreSequence.set(0)
        enabled = false
    }
}
