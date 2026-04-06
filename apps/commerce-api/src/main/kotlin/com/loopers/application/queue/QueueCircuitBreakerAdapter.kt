package com.loopers.application.queue

import com.loopers.domain.queue.QueueService
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 대기열 Redis 호출에 CircuitBreaker를 적용하는 어댑터.
 *
 * QueueService(Domain)에 직접 달 수 없는 이유:
 * - Domain 레이어에 인프라 기술(Resilience4j) 어노테이션 침투 금지
 * - CircuitBreaker는 AOP 프록시 기반이므로 반드시 별도 빈으로 분리
 *
 * isQueueEnabled() fallback → false 반환:
 * - Redis 장애 시 대기열 비활성화로 간주 → 주문 API 토큰 검증 생략 → 주문 통과
 */
@Component
class QueueCircuitBreakerAdapter(
    private val queueService: QueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "queue-redis", fallbackMethod = "isQueueEnabledFallback")
    fun isQueueEnabled(): Boolean = queueService.isQueueEnabled()

    @Suppress("UNUSED_PARAMETER")
    private fun isQueueEnabledFallback(e: Throwable): Boolean {
        log.warn("Redis 장애로 대기열 상태 조회 실패, 대기열 비활성화로 처리: {}", e.message)
        return false
    }
}
