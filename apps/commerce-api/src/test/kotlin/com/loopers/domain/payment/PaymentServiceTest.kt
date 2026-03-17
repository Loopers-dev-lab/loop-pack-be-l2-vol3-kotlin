package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("PaymentService Test")
class PaymentServiceTest {

    private val paymentRepository: PaymentRepository = mockk()
    private val paymentService = PaymentService(paymentRepository)

    @Test
    @DisplayName("결제를 거래 ID로 조회")
    fun getPaymentByTransactionId_success() {
        // given
        val payment = Payment.create(1L, "TXN001", BigDecimal("10000"))
        every { paymentRepository.findByTransactionId("TXN001") } returns payment

        // when
        val result = paymentService.getPaymentByTransactionId("TXN001")

        // then
        assert(result.id == payment.id)
        verify { paymentRepository.findByTransactionId("TXN001") }
    }

    @Test
    @DisplayName("결제를 거래 ID로 조회 - 존재하지 않으면 예외 발생")
    fun getPaymentByTransactionId_notFound() {
        // given
        every { paymentRepository.findByTransactionId("INVALID") } returns null

        // when & then
        assertThrows<CoreException> {
            paymentService.getPaymentByTransactionId("INVALID")
        }
    }

    @Test
    @DisplayName("주문 ID로 결제 조회")
    fun getPaymentByOrderId() {
        // given
        val payment = Payment.create(1L, "TXN001", BigDecimal("10000"))
        every { paymentRepository.findByOrderId(1L) } returns payment

        // when
        val result = paymentService.getPaymentByOrderId(1L)

        // then
        assert(result?.id == payment.id)
    }

    @Test
    @DisplayName("결제 저장")
    fun save() {
        // given
        val payment = Payment.create(1L, "TXN001", BigDecimal("10000"))
        every { paymentRepository.save(payment) } returns payment

        // when
        val result = paymentService.save(payment)

        // then
        assert(result.id == payment.id)
        verify { paymentRepository.save(payment) }
    }

    @Test
    @DisplayName("결제 생성")
    fun createPayment() {
        // given
        val payment = Payment.create(1L, "TXN001", BigDecimal("10000"))
        every { paymentRepository.save(any()) } returns payment

        // when
        val result = paymentService.createPayment(1L, "TXN001", BigDecimal("10000"))

        // then
        assert(result.orderId == 1L)
        assert(result.transactionId == "TXN001")
        verify { paymentRepository.save(any()) }
    }
}
