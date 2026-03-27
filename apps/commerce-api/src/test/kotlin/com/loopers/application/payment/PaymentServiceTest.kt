package com.loopers.application.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @InjectMocks
    private lateinit var paymentService: PaymentService

    @DisplayName("결제를 생성할 때,")
    @Nested
    inner class CreatePayment {

        @DisplayName("정상적인 정보가 주어지면, 카드번호가 마스킹되어 저장된다.")
        @Test
        fun createsPayment_withMaskedCardNo() {
            // arrange
            whenever(paymentRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = paymentService.createPayment(
                userId = 1L,
                orderId = 20260318000001L,
                amount = BigDecimal("50000"),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // assert
            assertThat(result.cardNo).isEqualTo("****-****-****-1451")
        }
    }

    @DisplayName("결제를 조회할 때,")
    @Nested
    inner class GetPayment {

        @DisplayName("orderId로 조회하면, 해당 결제가 반환된다.")
        @Test
        fun returnsPayment_whenFoundByOrderId() {
            // arrange
            val payment = Payment(
                orderId = 20260318000001L,
                userId = 1L,
                amount = BigDecimal("50000"),
                cardType = "SAMSUNG",
                cardNo = "****-****-****-1451",
            )
            whenever(paymentRepository.findByOrderId(20260318000001L)).thenReturn(payment)

            // act
            val result = paymentService.getPaymentByOrderId(20260318000001L)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.orderId).isEqualTo(20260318000001L)
        }

        @DisplayName("transactionKey로 조회하면, 해당 결제가 반환된다.")
        @Test
        fun returnsPayment_whenFoundByTransactionKey() {
            // arrange
            val payment = Payment(
                orderId = 20260318000001L,
                userId = 1L,
                amount = BigDecimal("50000"),
                cardType = "SAMSUNG",
                cardNo = "****-****-****-1451",
            )
            payment.markRequested("20260318:TR:abc123")
            whenever(paymentRepository.findByTransactionKey("20260318:TR:abc123")).thenReturn(payment)

            // act
            val result = paymentService.getPaymentByTransactionKey("20260318:TR:abc123")

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.transactionKey).isEqualTo("20260318:TR:abc123")
        }

        @DisplayName("존재하지 않는 paymentId로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenPaymentNotExists() {
            // arrange
            whenever(paymentRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                paymentService.getPayment(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
