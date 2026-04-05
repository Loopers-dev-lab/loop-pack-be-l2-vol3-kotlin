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

    /**
     * 토큰 발급 실패 등으로 대기열에서 이탈한 유저를 최우선 순위로 재삽입한다.
     */
    fun requeue(userIds: List<Long>)

    /**
     * 대기열에서 N명을 꺼내고 각각에게 입장 토큰을 원자적으로 발급한다.
     * ZPOPMIN + SET entry-token을 단일 Lua 스크립트로 실행하여
     * 중간 장애 시 유저 유실을 방지한다.
     *
     * @param count 꺼낼 유저 수
     * @param tokens 미리 생성된 토큰 목록 (count 이상)
     * @param tokenTtlSeconds 토큰 만료 시간 (초)
     * @return 입장된 userId → token 맵
     */
    fun popFrontAndIssueTokens(count: Long, tokens: List<String>, tokenTtlSeconds: Long): Map<Long, String>
}
