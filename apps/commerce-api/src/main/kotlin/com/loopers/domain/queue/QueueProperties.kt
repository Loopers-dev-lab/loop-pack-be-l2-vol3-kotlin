package com.loopers.domain.queue

/**
 * 대기열 설정값.
 * Infrastructure에서 @ConfigurationProperties로 주입한다.
 */
data class QueueProperties(
    /** 대기열 최대 인원 (0이면 무제한) */
    val maxQueueSize: Long = 50_000L,
    /** 입장 토큰 TTL (초) */
    val tokenTtlSeconds: Long = 300L,
    /** 스케줄러 1회당 토큰 발급 수 */
    val batchSize: Int = 18,
    /** 최대 동시 활성 토큰 수 (처리량 기준) */
    val maxActiveTokens: Long = 175L,
    /** 초당 처리량 (예상 대기 시간 계산용) */
    val throughputPerSecond: Double = 175.0,
    /** 스케줄러 활성화 여부. 다중 인스턴스 환경에서 단일 인스턴스만 true로 설정. */
    val scheduler: Scheduler = Scheduler(),
    /**
     * 처리 중 상태 만료 시간 (초).
     * 이 시간 동안 토큰이 발급되지 않으면 복구 스케줄러가 waiting으로 되돌린다.
     * SQS Visibility Timeout과 동일한 개념.
     */
    val processingTimeoutSeconds: Long = 30L,
) {
    data class Scheduler(
        val enabled: Boolean = true,
    )
}
