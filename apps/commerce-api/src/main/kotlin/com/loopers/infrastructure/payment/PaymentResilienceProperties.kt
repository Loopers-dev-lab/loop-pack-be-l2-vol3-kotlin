package com.loopers.infrastructure.payment

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "payment.resilience")
data class PaymentResilienceProperties(
    var timeoutDuration: Duration = Duration.ofSeconds(3),
    var circuitBreakerSlidingWindowSize: Int = 4,
    var circuitBreakerMinimumNumberOfCalls: Int = 2,
    var circuitBreakerFailureRateThreshold: Float = 50f,
    var circuitBreakerWaitDurationInOpenState: Duration = Duration.ofSeconds(30),
    var circuitBreakerPermittedNumberOfCallsInHalfOpenState: Int = 2,
)
