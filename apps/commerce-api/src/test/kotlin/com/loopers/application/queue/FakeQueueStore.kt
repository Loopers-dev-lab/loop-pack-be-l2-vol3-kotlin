package com.loopers.application.queue

class FakeQueueStore : QueueStore {

    private data class Entry(val memberId: Long, val score: Double)

    private val entries = mutableListOf<Entry>()
    private val memberSet = mutableSetOf<Long>()

    override fun add(memberId: Long, score: Double): Boolean {
        if (!memberSet.add(memberId)) return false
        entries.add(Entry(memberId, score))
        entries.sortBy { it.score }
        return true
    }

    override fun rank(memberId: Long): Long? {
        if (memberId !in memberSet) return null
        return entries.indexOfFirst { it.memberId == memberId }.toLong()
    }

    override fun size(): Long = entries.size.toLong()

    override fun popMin(count: Long): List<Long> {
        val result = mutableListOf<Long>()
        repeat(count.toInt().coerceAtMost(entries.size)) {
            val entry = entries.removeFirst()
            memberSet.remove(entry.memberId)
            result.add(entry.memberId)
        }
        return result
    }

    override fun rankFromMaster(memberId: Long): Long? = rank(memberId)

    override fun remove(memberId: Long): Boolean {
        if (!memberSet.remove(memberId)) return false
        entries.removeAll { it.memberId == memberId }
        return true
    }
}
