package com.loopers.domain.queue

interface OrderQueueRepository {

    /**
     * @return true: 신규 등록, false: 이미 대기열에 존재
     */
    fun enqueue(userId: Long, score: Double): Boolean

    /**
     * @return 0-based 순번, 대기열에 없으면 null
     */
    fun getPosition(userId: Long): Long?

    fun getTotalSize(): Long

    /**
     * @return 꺼낸 userId 목록 (진입 순서)
     */
    fun popFront(count: Long): List<Long>
}
