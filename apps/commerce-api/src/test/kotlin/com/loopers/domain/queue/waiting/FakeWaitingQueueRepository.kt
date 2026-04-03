package com.loopers.domain.queue.waiting

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.waiting.model.EnterResult
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import java.util.TreeMap

class FakeWaitingQueueRepository(
    private val entryTokenLookup: (UserId) -> String? = { null },
) : WaitingQueueRepository {

    private val scoreByUserId = mutableMapOf<UserId, Double>()
    private val sortedEntries = TreeMap<Double, MutableList<UserId>>()

    override fun enter(userId: UserId, maxCapacity: Int): EnterResult {
        if (entryTokenLookup(userId) != null) {
            return EnterResult.AlreadyHasToken
        }
        if (userId in scoreByUserId) {
            return EnterResult.Entered(requireNotNull(findPosition(userId)) { "대기열에 존재하는 사용자의 순번이 조회되지 않습니다. userId=$userId" })
        }
        if (scoreByUserId.size >= maxCapacity) {
            return EnterResult.QueueFull
        }
        // 단위 테스트에서는 nanoTime으로 진입 순서를 보장한다 (실제 Redis TIME과 동일한 FIFO 의미)
        val score = System.nanoTime().toDouble()
        scoreByUserId[userId] = score
        sortedEntries.getOrPut(score) { mutableListOf() }.add(userId)
        return EnterResult.Entered(requireNotNull(findPosition(userId)) { "대기열 진입 직후 순번이 조회되지 않습니다. userId=$userId" })
    }

    override fun findPosition(userId: UserId): Long? {
        val userScore = scoreByUserId[userId] ?: return null
        var rank = 0L
        for ((score, userIds) in sortedEntries) {
            for (uid in userIds) {
                if (uid == userId) return rank
                rank++
            }
            if (score > userScore) break
        }
        return null
    }

    override fun count(): Long = scoreByUserId.size.toLong()

    /**
     * 테스트 전용: FakeQueueTokenBatchProcessor에서 사용.
     * WaitingQueueRepository 인터페이스에는 없지만, Fake 내부에서 popMin 동작이 필요하므로 유지.
     */
    fun popMin(count: Int): List<UserId> {
        val result = mutableListOf<UserId>()
        repeat(count) {
            val firstEntry = sortedEntries.firstEntry() ?: return result
            val userIds = firstEntry.value
            // T-05: 동점 시 사전식 정렬로 pop
            val minUserId = userIds.minBy { it.value.toString() }
            userIds.remove(minUserId)
            if (userIds.isEmpty()) {
                sortedEntries.pollFirstEntry()
            }
            scoreByUserId.remove(minUserId)
            result.add(minUserId)
        }
        return result
    }
}
