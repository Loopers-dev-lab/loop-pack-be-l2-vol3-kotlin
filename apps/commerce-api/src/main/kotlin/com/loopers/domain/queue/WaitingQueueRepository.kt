package com.loopers.domain.queue

interface WaitingQueueRepository {
    fun enqueue(userId: Long, score: Double): Boolean
    fun getPosition(userId: Long): Long?
    fun getQueueSize(): Long
    fun dequeueTopN(count: Long): List<Long>
    fun remove(userId: Long)
}
