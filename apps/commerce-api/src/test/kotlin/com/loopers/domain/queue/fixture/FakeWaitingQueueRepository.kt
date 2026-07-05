package com.loopers.domain.queue.fixture

import com.loopers.domain.queue.WaitingQueueRepository

/**
 * Redis ZSET 의미론을 따르는 Fake.
 * 같은 score를 가진 멤버를 모두 보관하고, 동점이면 member 사전순으로 정렬한다.
 * (score를 Map key로 쓰면 같은 밀리초에 진입한 유저가 서로를 덮어쓴다)
 */
class FakeWaitingQueueRepository : WaitingQueueRepository {

    private val memberScores = mutableMapOf<Long, Double>()

    override fun enqueue(userId: Long, score: Double): Boolean {
        if (memberScores.containsKey(userId)) return false
        memberScores[userId] = score
        return true
    }

    override fun getPosition(userId: Long): Long? {
        if (!memberScores.containsKey(userId)) return null
        return sortedMembers().indexOf(userId).toLong()
    }

    override fun getQueueSize(): Long = memberScores.size.toLong()

    override fun dequeueTopN(count: Long): List<Long> {
        if (count <= 0) return emptyList()
        val dequeued = sortedMembers().take(count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        dequeued.forEach { memberScores.remove(it) }
        return dequeued
    }

    override fun remove(userId: Long) {
        memberScores.remove(userId)
    }

    private fun sortedMembers(): List<Long> {
        return memberScores.entries
            .sortedWith(compareBy({ it.value }, { it.key.toString() }))
            .map { it.key }
    }
}
