package com.loopers.infrastructure.payment.pg

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal

@DisplayName("LoopPaymentClient Unit Test")
class LoopPaymentClientTest {

    private val webClient: WebClient = mockk()
    private val client = LoopPaymentClient(
        webClient,
        "http://localhost:8083",
        "http://localhost:8080/api/v1/payments/callback",
        // readTimeoutSec
        1L,
    )

    @Test
    @DisplayName("paymentFallback - 예외 발생")
    fun paymentFallback_throwsCoreException() {
        // given
        val userId = 1L
        val transactionId = "TXN_123"
        val orderId = 100L
        val amount = BigDecimal("10000")
        val cardType = "SAMSUNG"
        val cardNo = "1234-5678-9814-1451"
        val exception = RuntimeException("PG service down")

        // when & then
        val thrownException = assertThrows<CoreException> {
            client.paymentFallback(
                userId,
                transactionId,
                orderId,
                amount,
                cardType,
                cardNo,
                exception,
            )
        }

        // then
        assert(thrownException.errorType == ErrorType.INTERNAL_ERROR)
        assert(thrownException.message!!.contains("unavailable"))
    }

    @Test
    @DisplayName("paymentFallback - 모든 예외 타입에 대해 동일하게 CoreException 발생")
    fun paymentFallback_throwsCoreExceptionForAnyException() {
        // given
        val userId = 1L
        val transactionId = "TXN_456"
        val orderId = 200L
        val amount = BigDecimal("20000")
        val cardType = "HYUNDAI"
        val cardNo = "5432-1098-7654-3210"
        val exception = IllegalStateException("Network timeout")

        // when & then
        val thrownException = assertThrows<CoreException> {
            client.paymentFallback(
                userId,
                transactionId,
                orderId,
                amount,
                cardType,
                cardNo,
                exception,
            )
        }

        // then
        assert(thrownException.errorType == ErrorType.INTERNAL_ERROR)
    }
}
