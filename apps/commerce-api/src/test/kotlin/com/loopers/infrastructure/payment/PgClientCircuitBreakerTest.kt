package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.support.error.CoreException
import com.ninjasquad.springmockk.MockkBean
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.RestTemplate

@SpringBootTest
@TestPropertySource(
    properties = [
        "resilience4j.circuitbreaker.instances.pgClient.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.pgClient.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.pgClient.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.pgClient.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.pgClient.permitted-number-of-calls-in-half-open-state=2",
        "resilience4j.circuitbreaker.instances.pgClient.automatic-transition-from-open-to-half-open-enabled=true",
    ],
)
class PgClientCircuitBreakerTest @Autowired constructor(
    private val pgClientImpl: PgClientImpl,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    @MockkBean(name = "pgRestTemplate")
    private lateinit var pgRestTemplate: RestTemplate

    companion object {
        private val TEST_REQUEST = PgPaymentRequest(
            orderId = "1",
            userId = 1L,
            amount = 50000L,
            callbackUrl = "http://localhost:8080/internal/v1/payments/callback",
            cardType = "VISA",
            cardNo = "4111111111111111",
        )
    }

    private lateinit var circuitBreaker: CircuitBreaker

    @BeforeEach
    fun setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgClient")
        circuitBreaker.reset()
    }

    private fun stubPgFailure() {
        every { pgRestTemplate.postForEntity(any<String>(), any(), any<Class<*>>()) } throws
            RuntimeException("Connection refused")
    }

    private fun stubPgSuccess() {
        every { pgRestTemplate.postForEntity(any<String>(), any(), any<Class<*>>()) } returns
            ResponseEntity.ok(PgPaymentResponse(transactionKey = "txn-test", status = "PENDING"))
    }

    @DisplayName("CircuitBreaker 상태 전이")
    @Nested
    inner class StateTransition {
        @DisplayName("초기 상태는 CLOSED이다.")
        @Test
        fun initialStateIsClosed() {
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)
        }

        @DisplayName("실패율이 임계값(50%)을 넘으면, OPEN으로 전이된다.")
        @Test
        fun transitionsToOpenWhenFailureRateExceedsThreshold() {
            // sliding-window-size=4, failure-rate-threshold=50%
            // 4번 호출 전부 실패 → 100% → OPEN
            stubPgFailure()

            repeat(4) {
                assertThrows<CoreException> { pgClientImpl.requestPayment(TEST_REQUEST) }
            }

            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)
        }

        @DisplayName("OPEN 상태에서는 호출 자체를 차단하고 즉시 fallback이 실행된다.")
        @Test
        fun rejectsCallsImmediatelyWhenOpen() {
            circuitBreaker.transitionToOpenState()

            // PG에 실제 HTTP 요청을 보내지 않고 즉시 fallback
            val exception = assertThrows<CoreException> {
                pgClientImpl.requestPayment(TEST_REQUEST)
            }
            assertThat(exception.message).contains("PG 서버가 일시적으로 불안정합니다")
        }

        @DisplayName("OPEN → waitDuration(1s) 경과 후 HALF_OPEN으로 전이된다.")
        @Test
        fun transitionsToHalfOpenAfterWaitDuration() {
            circuitBreaker.transitionToOpenState()

            // waitDuration=1s 대기
            Thread.sleep(1_500)

            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.HALF_OPEN)
        }

        @DisplayName("HALF_OPEN에서 허용된 호출이 모두 실패하면, 다시 OPEN으로 전이된다.")
        @Test
        fun transitionsBackToOpenWhenHalfOpenCallsFail() {
            stubPgFailure()

            circuitBreaker.transitionToOpenState()
            circuitBreaker.transitionToHalfOpenState()

            // permitted-number-of-calls-in-half-open-state=2, 2번 모두 실패
            repeat(2) {
                assertThrows<CoreException> { pgClientImpl.requestPayment(TEST_REQUEST) }
            }

            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)
        }

        @DisplayName("HALF_OPEN에서 허용된 호출이 모두 성공하면, CLOSED로 복귀한다.")
        @Test
        fun transitionsToClosedWhenHalfOpenCallsSucceed() {
            stubPgSuccess()

            circuitBreaker.transitionToOpenState()
            circuitBreaker.transitionToHalfOpenState()

            // 2번 모두 성공
            repeat(2) {
                pgClientImpl.requestPayment(TEST_REQUEST)
            }

            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)
        }
    }

    @DisplayName("Fallback 동작")
    @Nested
    inner class Fallback {
        @DisplayName("PG 호출 실패 시 fallback이 CoreException을 던진다.")
        @Test
        fun fallbackThrowsCoreExceptionOnFailure() {
            stubPgFailure()

            val exception = assertThrows<CoreException> {
                pgClientImpl.requestPayment(TEST_REQUEST)
            }
            assertThat(exception.message).contains("PG 서버가 일시적으로 불안정합니다")
        }
    }
}
