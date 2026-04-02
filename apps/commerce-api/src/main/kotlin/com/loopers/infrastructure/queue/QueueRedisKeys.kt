package com.loopers.infrastructure.queue

/**
 * 대기열 관련 Redis 키 정의.
 * Redis Cluster 환경에서 Lua 스크립트가 동작하려면
 * 같은 슬롯에 배치되어야 하므로 {queue} 해시태그를 사용한다.
 */
object QueueRedisKeys {
    /** 대기열 Sorted Set 키 */
    const val WAITING_QUEUE = "{queue}:waiting"

    /** 입장 토큰 키 prefix. 실제 키: {queue}:token:{userId} */
    fun tokenKey(userId: Long): String = "{queue}:token:$userId"

    /** 활성 토큰 수 추적 키 */
    const val ACTIVE_TOKEN_COUNT = "{queue}:active-count"

    /** 대기열 활성화 플래그 키 */
    const val ENABLED = "{queue}:enabled"

    /**
     * 처리 중 상태 Sorted Set 키.
     * score = claimed_at (밀리초). 일정 시간 초과 시 복구 스케줄러가 waiting으로 복구.
     * SQS Visibility Timeout과 동일한 원리.
     */
    const val PROCESSING_QUEUE = "{queue}:processing"
}
