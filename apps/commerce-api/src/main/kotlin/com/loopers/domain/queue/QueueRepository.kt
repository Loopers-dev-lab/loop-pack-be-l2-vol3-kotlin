package com.loopers.domain.queue

/**
 * 대기열(Redis Sorted Set) 추상화.
 * Domain Layer에서는 Redis를 직접 알지 않으며,
 * Infrastructure Layer의 구현체가 Redis 명령을 수행한다.
 */
interface QueueRepository {

    /** 대기열에 유저를 추가한다. 이미 존재하면 false를 반환하고 score를 갱신하지 않는다 (NX). */
    fun add(userId: Long): Boolean

    /** 유저의 현재 순번을 조회한다. 대기열에 없으면 null. (0-based) */
    fun getRank(userId: Long): Long?

    /** 현재 대기열의 전체 인원을 조회한다. */
    fun size(): Long

    /** 대기열 앞쪽에서 count명을 꺼낸다 (꺼내면서 삭제). */
    fun popMin(count: Long): List<QueueEntry>

    /** 대기열 활성화 여부를 조회한다. */
    fun isEnabled(): Boolean

    /** 대기열을 활성화한다. */
    fun enable()

    /** 대기열을 비활성화한다. */
    fun disable()

    /**
     * 대기열에서 N명을 꺼내 처리 중 상태로 원자적으로 이동한다 (Lua).
     * - waiting에서 ZPOPMIN → processing에 ZADD (score=claimed_at 밀리초)
     * - 서버가 죽어도 processing에 남아있어 복구 가능.
     */
    fun moveToProcessing(count: Long): List<QueueEntry>

    /**
     * 처리 완료된 항목을 처리 중 상태에서 제거한다.
     * 토큰 발급 성공 후 호출.
     */
    fun removeFromProcessing(userIds: List<Long>)

    /**
     * 처리 중 상태에서 만료된 항목을 waiting으로 복구한다 (Lua 원자적).
     * - beforeTimestampMs 이전에 claimed된 항목을 복구 대상으로 판단.
     * - ZADD NX로 복구 → 유저가 이미 재진입했으면 덮어쓰지 않음.
     * @return 복구된 인원 수
     */
    fun recoverExpiredToWaiting(beforeTimestampMs: Long): Int
}
