package com.loopers.domain.queue.waiting.repository

import com.loopers.domain.common.vo.UserId

interface WaitingQueueRepository {

    /**
     * 대기열에 진입한다.
     * Sorted Set 특성으로 이미 존재하는 userId는 score가 갱신되지 않는다 (NX).
     * score는 Redis 서버 타임스탬프 기반으로 내부에서 원자적으로 생성된다.
     * 상한 초과 시 null을 반환한다.
     *
     * @return 진입 후 0-based 순번. 상한 초과 시 null.
     */
    fun enter(userId: UserId, maxCapacity: Int): Long?

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

    /**
     * 대기열 앞에서 N명을 꺼낸다 (ZPOPMIN).
     *
     * @return 꺼낸 UserId 리스트 (score 오름차순).
     */
    fun popMin(count: Int): List<UserId>

    /**
     * 대기열 앞에서 N명을 원자적으로 꺼내고 각 userId에 입장 토큰을 발급한다.
     * "절대로 먼저 삭제하지 마라" 원칙: SET entry-token 후 ZREM 수행.
     *
     * @param count 꺼낼 인원 수
     * @param ttlSeconds 토큰 TTL (초)
     * @return (UserId, token) 쌍의 리스트
     */
    fun popMinAndIssueTokens(count: Int, ttlSeconds: Long): List<Pair<UserId, String>>
}
