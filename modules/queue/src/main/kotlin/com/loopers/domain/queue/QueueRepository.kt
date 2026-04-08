package com.loopers.domain.queue

interface QueueRepository {
    fun addIfAbsent(userId: Long, score: Double): Boolean
    fun getRank(userId: Long): Long?
    fun getSize(): Long
    fun popMin(count: Long): Set<String>
    fun remove(userId: Long): Boolean
}
