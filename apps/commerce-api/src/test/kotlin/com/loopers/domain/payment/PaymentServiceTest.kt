package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("PaymentService")
class PaymentServiceTest {

    private val paymentRepository: PaymentRepository = mockk()
    private val paymentService = PaymentService(paymentRepository)

    companion object {
        private const val PAYMENT_ID = 1L
        private const val ORDER_ID = 100L
        private const val PAYMENT_AMOUNT = 50_000L
    }

    private fun createPayment(
        id: Long = PAYMENT_ID,
        orderId: Long = ORDER_ID,
        amount: Long = PAYMENT_AMOUNT,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusMinutes(30),
    ): PaymentModel {
        val payment = PaymentModel(
            orderId = orderId,
            amount = amount,
            expiresAt = expiresAt,
        )
        return spyk(payment) {
            every { this@spyk.id } returns id
        }
    }

    @DisplayName("createPayment")
    @Nested
    inner class CreatePayment {
        @DisplayName("결제를 생성하면 저장 후 반환한다")
        @Test
        fun savesAndReturnsPayment_whenCreatePaymentIsCalled() {
            val expiresAt = ZonedDateTime.now().plusMinutes(30)
            every { paymentRepository.save(any()) } answers { firstArg() }

            val result = paymentService.createPayment(
                orderId = ORDER_ID,
                amount = PAYMENT_AMOUNT,
                expiresAt = expiresAt,
            )

            assertThat(result.orderId).isEqualTo(ORDER_ID)
            assertThat(result.amount).isEqualTo(PAYMENT_AMOUNT)
            assertThat(result.status).isEqualTo(PaymentStatus.PENDING)
            assertThat(result.expiresAt).isEqualTo(expiresAt)
            verify(exactly = 1) { paymentRepository.save(any()) }
        }
    }

    @DisplayName("findById")
    @Nested
    inner class FindById {
        @DisplayName("존재하는 결제 ID로 조회하면 결제를 반환한다")
        @Test
        fun returnsPayment_whenPaymentExists() {
            val payment = createPayment()
            every { paymentRepository.findByIdAndDeletedAtIsNull(PAYMENT_ID) } returns payment

            val result = paymentService.findById(PAYMENT_ID)

            assertThat(result).isSameAs(payment)
        }

        @DisplayName("존재하지 않는 결제 ID로 조회하면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFound_whenPaymentDoesNotExist() {
            every { paymentRepository.findByIdAndDeletedAtIsNull(PAYMENT_ID) } returns null

            assertThatThrownBy {
                paymentService.findById(PAYMENT_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
                .hasMessageContaining("존재하지 않는 결제입니다")
        }
    }

    @DisplayName("markSucceeded")
    @Nested
    inner class MarkSucceeded {
        @DisplayName("결제 성공 처리 후 저장한다")
        @Test
        fun savesPayment_whenPaymentIsMarkedSucceeded() {
            val payment = createPayment()
            every { paymentRepository.findByIdAndDeletedAtIsNull(PAYMENT_ID) } returns payment
            every { paymentRepository.save(payment) } returns payment

            val result = paymentService.markSucceeded(PAYMENT_ID, "tx-001")

            assertThat(result.status).isEqualTo(PaymentStatus.SUCCEEDED)
            assertThat(result.externalTransactionId).isEqualTo("tx-001")
            verify(exactly = 1) { paymentRepository.save(payment) }
        }
    }

    @DisplayName("markFailed")
    @Nested
    inner class MarkFailed {
        @DisplayName("결제 실패 처리 후 저장한다")
        @Test
        fun savesPayment_whenPaymentIsMarkedFailed() {
            val payment = createPayment()
            every { paymentRepository.findByIdAndDeletedAtIsNull(PAYMENT_ID) } returns payment
            every { paymentRepository.save(payment) } returns payment

            val result = paymentService.markFailed(PAYMENT_ID, "pg error")

            assertThat(result.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(result.failureReason).isEqualTo("pg error")
            verify(exactly = 1) { paymentRepository.save(payment) }
        }
    }

    @DisplayName("markExpired")
    @Nested
    inner class MarkExpired {
        @DisplayName("결제 만료 처리 후 저장한다")
        @Test
        fun savesPayment_whenPaymentIsMarkedExpired() {
            val payment = createPayment()
            every { paymentRepository.findByIdAndDeletedAtIsNull(PAYMENT_ID) } returns payment
            every { paymentRepository.save(payment) } returns payment

            val result = paymentService.markExpired(PAYMENT_ID)

            assertThat(result.status).isEqualTo(PaymentStatus.EXPIRED)
            verify(exactly = 1) { paymentRepository.save(payment) }
        }
    }
}
