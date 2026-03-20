package com.loopers.infrastructure.payment

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
@EnableConfigurationProperties(PaymentResilienceProperties::class)
class PaymentResilienceConfig {
    @Bean(destroyMethod = "close")
    fun paymentResilienceExecutor(): ExecutorService {
        return Executors.newVirtualThreadPerTaskExecutor()
    }

    @Bean
    fun paymentCircuitBreaker(properties: PaymentResilienceProperties): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .slidingWindowSize(properties.circuitBreakerSlidingWindowSize)
            .minimumNumberOfCalls(properties.circuitBreakerMinimumNumberOfCalls)
            .failureRateThreshold(properties.circuitBreakerFailureRateThreshold)
            .waitDurationInOpenState(properties.circuitBreakerWaitDurationInOpenState)
            .permittedNumberOfCallsInHalfOpenState(properties.circuitBreakerPermittedNumberOfCallsInHalfOpenState)
            .recordException { throwable ->
                throwable !is com.loopers.support.error.CoreException ||
                    throwable.errorType == com.loopers.support.error.ErrorType.INTERNAL_ERROR
            }
            .build()
        return CircuitBreaker.of("paymentGateway", config)
    }

    @Bean
    fun paymentTimeLimiter(properties: PaymentResilienceProperties): TimeLimiter {
        val config = TimeLimiterConfig.custom()
            .timeoutDuration(properties.timeoutDuration)
            .cancelRunningFuture(true)
            .build()
        return TimeLimiter.of(config)
    }

    @Bean
    @Primary
    fun resilientPgClient(
        @Qualifier("delegatePgClient") delegate: PgClient,
        properties: PaymentResilienceProperties,
        paymentCircuitBreaker: CircuitBreaker,
        paymentTimeLimiter: TimeLimiter,
        @Qualifier("paymentResilienceExecutor") executor: ExecutorService,
    ): PgClient {
        return ResilientPgClient(delegate, properties, paymentCircuitBreaker, paymentTimeLimiter, executor)
    }
}
