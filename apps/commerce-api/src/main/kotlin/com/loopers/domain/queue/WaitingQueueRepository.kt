package com.loopers.domain.queue

interface WaitingQueueRepository {
    fun enter(userId: Long, score: Double): Boolean
    fun getPosition(userId: Long): Long?
    fun getTotalWaitingCount(): Long
    fun popMinN(count: Long): Set<String>
    fun exists(userId: Long): Boolean
}
