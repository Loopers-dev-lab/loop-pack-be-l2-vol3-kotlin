package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("Payment Entity Test")
class PaymentTest {

    @Test
    @DisplayName("결제 완료 - 금액이 일치하면 성공")
    fun markAsCompleted_success() {
        // given
        val payment = Payment.create(
            orderId = 1L,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
        )

        // when
        payment.markAsCompleted(BigDecimal("10000"))

        // then
        assert(payment.status == PaymentStatus.COMPLETED)
    }

    @Test
    @DisplayName("결제 완료 - 금액이 불일치하면 실패")
    fun markAsCompleted_amountMismatch() {
        // given
        val payment = Payment.create(
            orderId = 1L,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
        )

        // when & then
        assertThrows<CoreException> {
            payment.markAsCompleted(BigDecimal("9999"))
        }
    }

    @Test
    @DisplayName("결제 완료 - 이미 완료된 결제는 다시 완료할 수 없음")
    fun markAsCompleted_alreadyCompleted() {
        // given
        val payment = Payment.create(
            orderId = 1L,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
        )
        payment.markAsCompleted(BigDecimal("10000"))

        // when & then
        assertThrows<CoreException> {
            payment.markAsCompleted(BigDecimal("10000"))
        }
    }

    @Test
    @DisplayName("결제 실패 처리")
    fun markAsFailed() {
        // given
        val payment = Payment.create(
            orderId = 1L,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
        )

        // when
        payment.markAsFailed()

        // then
        assert(payment.status == PaymentStatus.FAILED)
    }

    @Test
    @DisplayName("결제 취소 처리")
    fun markAsCancelled() {
        // given
        val payment = Payment.create(
            orderId = 1L,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
        )

        // when
        payment.markAsCancelled()

        // then
        assert(payment.status == PaymentStatus.CANCELLED)
    }

    @Test
    @DisplayName("결제 생성 시 초기 상태는 INITIATED")
    fun creation_initialStatus() {
        // given & when
        val payment = Payment.create(
            orderId = 1L,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
        )

        // then
        assert(payment.status == PaymentStatus.INITIATED)
        assert(payment.orderId == 1L)
        assert(payment.transactionId == "TXN001")
        assert(payment.amount == BigDecimal("10000"))
    }
}
