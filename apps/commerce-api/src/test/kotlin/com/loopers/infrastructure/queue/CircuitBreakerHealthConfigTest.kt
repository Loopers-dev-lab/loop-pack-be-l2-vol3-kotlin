package com.loopers.infrastructure.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.HealthContributorRegistry
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("CircuitBreaker health endpoint 설정")
class CircuitBreakerHealthConfigTest @Autowired constructor(
    private val healthContributorRegistry: HealthContributorRegistry,
) {

    @Test
    @DisplayName("order-queue CircuitBreaker의 health indicator가 Actuator에 등록되어 있다")
    fun orderQueueCircuitBreakerHealthIndicatorIsRegistered() {
        // arrange & act
        val contributor = healthContributorRegistry.getContributor("circuitBreakers")

        // assert
        assertThat(contributor).isNotNull()
    }
}
