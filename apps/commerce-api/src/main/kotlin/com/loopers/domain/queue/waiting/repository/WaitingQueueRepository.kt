package com.loopers.domain.queue.waiting.repository

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.waiting.model.EnterResult

interface WaitingQueueRepository {

    /**
     * 대기열에 진입한다.
     * 이미 토큰을 보유한 사용자는 대기열에 다시 진입하지 않는다.
     * Sorted Set 특성으로 이미 존재하는 userId는 score가 갱신되지 않는다 (NX).
     * score는 Redis 서버 타임스탬프 기반으로 내부에서 원자적으로 생성된다.
     *
     * @return 진입 결과.
     */
    fun enter(userId: UserId, maxCapacity: Int): EnterResult

    /**
     * 대기열에서의 현재 순번을 조회한다.
     *
     * @return 0-based 순번. 대기열에 없으면 null.
     */
    fun findPosition(userId: UserId): Long?

    /**
     * 전체 대기 인원을 조회한다.
     */
    fun count(): Long
}
