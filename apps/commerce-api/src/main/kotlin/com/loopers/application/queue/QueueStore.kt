package com.loopers.application.queue

interface QueueStore {

    fun add(memberId: Long, score: Double): Boolean

    fun rank(memberId: Long): Long?

    fun rankFromMaster(memberId: Long): Long?

    fun size(): Long

    fun popMin(count: Long): List<Long>

    fun remove(memberId: Long): Boolean
}
