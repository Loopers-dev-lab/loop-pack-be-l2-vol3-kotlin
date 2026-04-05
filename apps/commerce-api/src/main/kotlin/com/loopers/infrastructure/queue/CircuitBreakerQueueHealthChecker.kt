package com.loopers.infrastructure.queue

import com.loopers.domain.queue.QueueHealthChecker
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.stereotype.Component

@Component
class CircuitBreakerQueueHealthChecker(
    circuitBreakerRegistry: CircuitBreakerRegistry,
) : QueueHealthChecker {

    private val circuitBreaker: CircuitBreaker =
        circuitBreakerRegistry.circuitBreaker(OrderQueueRedisRepository.CIRCUIT_BREAKER_NAME)

    // HALF_OPEN 상태에서도 bypass 처리: 허용된 호출 수 초과 시 CallNotPermittedException으로
    // 정당한 유저의 토큰 검증이 실패하는 문제 방지
    override fun isBypassed(): Boolean =
        circuitBreaker.state == CircuitBreaker.State.OPEN ||
            circuitBreaker.state == CircuitBreaker.State.HALF_OPEN
}
