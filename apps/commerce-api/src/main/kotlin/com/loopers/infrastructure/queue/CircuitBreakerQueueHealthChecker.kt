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

    override fun isBypassed(): Boolean =
        circuitBreaker.state == CircuitBreaker.State.OPEN
}
