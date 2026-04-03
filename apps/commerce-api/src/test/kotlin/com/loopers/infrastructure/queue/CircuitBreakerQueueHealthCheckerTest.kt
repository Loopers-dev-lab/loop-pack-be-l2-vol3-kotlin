package com.loopers.infrastructure.queue

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CircuitBreakerQueueHealthCheckerTest {

    @Mock
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Mock
    private lateinit var circuitBreaker: CircuitBreaker

    private lateinit var healthChecker: CircuitBreakerQueueHealthChecker

    @BeforeEach
    fun setUp() {
        whenever(circuitBreakerRegistry.circuitBreaker("order-queue")).thenReturn(circuitBreaker)
        healthChecker = CircuitBreakerQueueHealthChecker(circuitBreakerRegistry)
    }

    @Nested
    @DisplayName("isBypassed()를 호출할 때,")
    inner class IsBypassed {

        @Test
        @DisplayName("CircuitBreaker가 OPEN이면 true를 반환한다")
        fun returnsTrue_whenCircuitBreakerIsOpen() {
            // arrange
            whenever(circuitBreaker.state).thenReturn(CircuitBreaker.State.OPEN)

            // act
            val result = healthChecker.isBypassed()

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("CircuitBreaker가 CLOSED이면 false를 반환한다")
        fun returnsFalse_whenCircuitBreakerIsClosed() {
            // arrange
            whenever(circuitBreaker.state).thenReturn(CircuitBreaker.State.CLOSED)

            // act
            val result = healthChecker.isBypassed()

            // assert
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("CircuitBreaker가 HALF_OPEN이면 false를 반환한다")
        fun returnsFalse_whenCircuitBreakerIsHalfOpen() {
            // arrange
            whenever(circuitBreaker.state).thenReturn(CircuitBreaker.State.HALF_OPEN)

            // act
            val result = healthChecker.isBypassed()

            // assert
            assertThat(result).isFalse()
        }
    }
}
