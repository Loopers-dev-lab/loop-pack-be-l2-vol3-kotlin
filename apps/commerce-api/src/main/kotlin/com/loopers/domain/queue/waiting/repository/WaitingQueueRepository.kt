package com.loopers.domain.queue.waiting.repository

interface WaitingQueueRepository {

    /**
     * 대기열에 진입한다.
     * Sorted Set 특성으로 이미 존재하는 userId는 score가 갱신되지 않는다 (NX).
     * 상한 초과 시 null을 반환한다.
     *
     * @return 진입 후 0-based 순번. 상한 초과 시 null.
     */
    fun enter(userId: Long, score: Double, maxCapacity: Int): Long?

    /**
     * 대기열에서의 현재 순번을 조회한다.
     *
     * @return 0-based 순번. 대기열에 없으면 null.
     */
    fun findPosition(userId: Long): Long?

    /**
     * 전체 대기 인원을 조회한다.
     */
    fun count(): Long

    /**
     * 대기열 앞에서 N명을 꺼낸다 (ZPOPMIN).
     *
     * @return 꺼낸 userId 리스트 (score 오름차순).
     */
    fun popMin(count: Int): List<Long>
}
