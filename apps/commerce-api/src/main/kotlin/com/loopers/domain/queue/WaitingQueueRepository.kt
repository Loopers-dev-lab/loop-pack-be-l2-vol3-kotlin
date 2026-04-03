package com.loopers.domain.queue

interface WaitingQueueRepository {
    fun enter(userId: Long): QueuePosition
    fun getPosition(userId: Long): QueuePosition?
    fun size(): Long
    fun remove(userId: Long)
    fun popFront(count: Long): List<Long>
}
