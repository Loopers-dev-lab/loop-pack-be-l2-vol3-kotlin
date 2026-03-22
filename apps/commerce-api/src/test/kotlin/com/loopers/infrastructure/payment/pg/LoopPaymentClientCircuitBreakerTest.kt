package com.loopers.infrastructure.payment.pg

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LoopPaymentClient CircuitBreaker Integration Test")
class LoopPaymentClientCircuitBreakerTest {

    @Autowired
    private lateinit var loopPaymentClient: LoopPaymentClient

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Test
    @DisplayName("CircuitBreaker OPEN 상태 - fallback 호출되어 CoreException 발생")
    fun requestPayment_circuitBreakerOpen_callsFallback() {
        // given
        val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
        cb.transitionToOpenState()

        val userId = 1L
        val transactionId = "TXN_123"
        val orderId = 100L
        val amount = BigDecimal("10000")
        val cardType = "SAMSUNG"
        val cardNo = "1234-5678-9814-1451"

        // when & then
        val thrownException = assertThrows<CoreException> {
            loopPaymentClient.requestPayment(
                userId,
                transactionId,
                orderId,
                amount,
                cardType,
                cardNo,
            )
        }

        // then
        assert(thrownException.errorType == ErrorType.INTERNAL_ERROR)
        assert(thrownException.message!!.contains("unavailable"))

        // cleanup
        cb.transitionToClosedState()
    }

    @Test
    @DisplayName("CircuitBreaker OPEN 상태 - 여러 호출 모두 fallback 호출")
    fun requestPayment_circuitBreakerOpen_multipleCalls() {
        // given
        val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
        cb.transitionToOpenState()

        val userId = 1L
        val amount = BigDecimal("10000")

        // when & then
        repeat(3) { index ->
            val thrownException = assertThrows<CoreException> {
                loopPaymentClient.requestPayment(
                    userId,
                    "TXN_$index",
                    100L + index,
                    amount,
                    "SAMSUNG",
                    "1234-5678-9814-1451",
                )
            }
            assert(thrownException.errorType == ErrorType.INTERNAL_ERROR)
        }

        // cleanup
        cb.transitionToClosedState()
    }
}
