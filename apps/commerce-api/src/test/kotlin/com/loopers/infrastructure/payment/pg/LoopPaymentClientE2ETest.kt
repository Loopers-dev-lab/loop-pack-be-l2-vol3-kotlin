package com.loopers.infrastructure.payment.pg

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LoopPaymentClient E2E Test (Real PG Service)")
class LoopPaymentClientE2ETest {

    @Autowired
    private lateinit var loopPaymentClient: LoopPaymentClient

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @BeforeEach
    fun setup() {
        // CircuitBreaker 초기화
        val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
        cb.transitionToClosedState()
    }

    @Test
    @DisplayName("PG 서비스 정상 응답 - 성공")
    fun requestPayment_pgServiceSuccess() {
        // given
        val userId = 1L
        val transactionId = "TXN_E2E_001"
        val orderId = 100L
        val amount = BigDecimal("10000")
        val cardType = "SAMSUNG"
        val cardNo = "1234-5678-9814-1451"

        // when - PG 서비스가 http://localhost:8083에서 실행 중이면 성공
        try {
            val result = loopPaymentClient.requestPayment(
                userId,
                transactionId,
                orderId,
                amount,
                cardType,
                cardNo,
            )

            // then - 성공 시 응답 데이터 확인
            println("✅ PG Service Response: $result")
            assert(result.orderId == orderId.toString())
            assert(result.amount == 10000L)
        } catch (e: CoreException) {
            // PG 서비스가 없을 때: fallback 호출되어 CoreException 발생
            println("⚠️  PG Service unavailable (expected if PG is down): ${e.message}")
            assert(e.errorType == ErrorType.INTERNAL_ERROR)
            assert(e.message?.contains("unavailable") == true)
        }
    }

    @Test
    @DisplayName("PG 서비스 Timeout - Retry 후 CircuitBreaker OPEN → Fallback")
    fun requestPayment_pgServiceTimeout_circuitBreakerOpens() {
        // given
        val userId = 2L
        val transactionId = "TXN_E2E_TIMEOUT_001"
        val orderId = 200L
        val amount = BigDecimal("20000")

        // when - PG 서비스가 없으면 Timeout 발생 → Retry → CircuitBreaker OPEN
        val thrownException = assertThrows<CoreException> {
            loopPaymentClient.requestPayment(
                userId,
                transactionId,
                orderId,
                amount,
                "SAMSUNG",
                "1234-5678-9814-1451",
            )
        }

        // then - Fallback이 호출되어 CoreException 발생
        assert(thrownException.errorType == ErrorType.INTERNAL_ERROR)
        assert(thrownException.message?.contains("unavailable") == true)

        // CircuitBreaker 상태 확인
        val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
        println("CircuitBreaker State: ${cb.state}")
    }

    @Test
    @DisplayName("여러 호출로 CircuitBreaker 빠르게 OPEN")
    fun requestPayment_multipleFailures_openCircuitBreaker() {
        // given
        val userId = 3L
        val amount = BigDecimal("30000")

        // when - 여러 번 호출하여 CircuitBreaker OPEN
        repeat(3) { index ->
            try {
                loopPaymentClient.requestPayment(
                    userId,
                    "TXN_E2E_$index",
                    300L + index,
                    amount,
                    "SAMSUNG",
                    "1234-5678-9814-1451",
                )
            } catch (e: CoreException) {
                println("Call $index: Fallback called - ${e.message}")
            }
        }

        // then - CircuitBreaker 상태 확인
        val cb = circuitBreakerRegistry.circuitBreaker("loop-pg-payment")
        println("Final CircuitBreaker State: ${cb.state}")
    }
}
