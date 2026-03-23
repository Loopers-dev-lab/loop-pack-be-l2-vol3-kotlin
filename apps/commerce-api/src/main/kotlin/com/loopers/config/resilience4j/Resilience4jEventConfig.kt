package com.loopers.config.resilience4j

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.RetryRegistry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
class Resilience4jEventConfig(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry,
    private val rateLimiterRegistry: RateLimiterRegistry,
) {

    private val log = LoggerFactory.getLogger("Resilience4j")

    @PostConstruct
    fun registerEventListeners() {
        registerCircuitBreakerEvents()
        registerRetryEvents()
        registerRateLimiterEvents()
    }

    private fun registerCircuitBreakerEvents() {
        circuitBreakerRegistry.allCircuitBreakers.forEach { cb ->
            val name = cb.name

            cb.eventPublisher
                .onStateTransition { event ->
                    log.warn(
                        "[CircuitBreaker] {} 상태 전이: {} → {}",
                        name,
                        event.stateTransition.fromState,
                        event.stateTransition.toState,
                    )
                }
                .onFailureRateExceeded { event ->
                    log.warn(
                        "[CircuitBreaker] {} 실패율 초과: {}%",
                        name,
                        event.failureRate,
                    )
                }
                .onSlowCallRateExceeded { event ->
                    log.warn(
                        "[CircuitBreaker] {} 느린호출 비율 초과: {}%",
                        name,
                        event.slowCallRate,
                    )
                }
                .onCallNotPermitted {
                    log.warn("[CircuitBreaker] {} OPEN — 요청 차단됨", name)
                }
                .onError { event ->
                    log.info(
                        "[CircuitBreaker] {} 실패 기록 (소요: {}ms, 예외: {})",
                        name,
                        event.elapsedDuration.toMillis(),
                        event.throwable.message,
                    )
                }
                .onSuccess { event ->
                    log.info(
                        "[CircuitBreaker] {} 성공 (소요: {}ms)",
                        name,
                        event.elapsedDuration.toMillis(),
                    )
                }
        }
    }

    private fun registerRetryEvents() {
        retryRegistry.allRetries.forEach { retry ->
            val name = retry.name

            retry.eventPublisher
                .onRetry { event ->
                    log.warn(
                        "[Retry] {} 재시도 (attempt: {}/{}, 대기: {}ms, 예외: {})",
                        name,
                        event.numberOfRetryAttempts + 1,
                        retry.retryConfig.maxAttempts,
                        event.waitInterval,
                        event.lastThrowable?.message,
                    )
                }
                .onError { event ->
                    log.error(
                        "[Retry] {} 최종 실패 (총 {}회 시도, 예외: {})",
                        name,
                        event.numberOfRetryAttempts,
                        event.lastThrowable?.message,
                    )
                }
                .onSuccess { event ->
                    log.info(
                        "[Retry] {} 재시도 후 성공 (attempt: {})",
                        name,
                        event.numberOfRetryAttempts,
                    )
                }
        }
    }

    private fun registerRateLimiterEvents() {
        rateLimiterRegistry.allRateLimiters.forEach { rl ->
            val name = rl.name

            rl.eventPublisher
                .onSuccess {
                    log.info("[RateLimiter] {} 허용됨", name)
                }
                .onFailure {
                    log.warn("[RateLimiter] {} 거부됨 — 요청 제한 초과", name)
                }
        }
    }
}
