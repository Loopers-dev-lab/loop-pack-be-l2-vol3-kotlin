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
import org.springframework.dao.DataIntegrityViolationException
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
            every { paymentRepository.findByOrderIdAndDeletedAtIsNull(ORDER_ID) } returns null
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

        @DisplayName("같은 주문에 이미 결제가 있으면 CONFLICT 예외가 발생한다")
        @Test
        fun throwsConflict_whenPaymentAlreadyExistsForOrder() {
            val payment = createPayment(orderId = ORDER_ID)
            every { paymentRepository.findByOrderIdAndDeletedAtIsNull(ORDER_ID) } returns payment

            assertThatThrownBy {
                paymentService.createPayment(
                    orderId = ORDER_ID,
                    amount = PAYMENT_AMOUNT,
                    expiresAt = ZonedDateTime.now().plusMinutes(30),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
                .hasMessageContaining("이미 결제가 진행 중인 주문입니다")
        }

        @DisplayName("동시 요청으로 저장 시점 중복이 발생해도 CONFLICT 예외로 변환한다")
        @Test
        fun throwsConflict_whenDuplicateIsDetectedDuringSave() {
            every { paymentRepository.findByOrderIdAndDeletedAtIsNull(ORDER_ID) } returns null
            every { paymentRepository.save(any()) } throws DataIntegrityViolationException("duplicate key")

            assertThatThrownBy {
                paymentService.createPayment(
                    orderId = ORDER_ID,
                    amount = PAYMENT_AMOUNT,
                    expiresAt = ZonedDateTime.now().plusMinutes(30),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
                .hasMessageContaining("이미 결제가 진행 중인 주문입니다")
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

    @DisplayName("findByOrderId")
    @Nested
    inner class FindByOrderId {
        @DisplayName("존재하는 주문 ID로 조회하면 결제를 반환한다")
        @Test
        fun returnsPayment_whenPaymentExistsForOrder() {
            val payment = createPayment(orderId = ORDER_ID)
            every { paymentRepository.findByOrderIdAndDeletedAtIsNull(ORDER_ID) } returns payment

            val result = paymentService.findByOrderId(ORDER_ID)

            assertThat(result).isSameAs(payment)
        }

        @DisplayName("존재하지 않는 주문 ID로 조회하면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFound_whenPaymentDoesNotExistForOrder() {
            every { paymentRepository.findByOrderIdAndDeletedAtIsNull(ORDER_ID) } returns null

            assertThatThrownBy {
                paymentService.findByOrderId(ORDER_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
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

    @DisplayName("expirePendingPayments")
    @Nested
    inner class ExpirePendingPayments {
        @DisplayName("만료 시각이 지난 PENDING 결제만 EXPIRED로 바꾸고 저장한다")
        @Test
        fun expiresOnlyOverduePendingPayments() {
            val overduePayment = createPayment(id = 1L, expiresAt = ZonedDateTime.now().minusMinutes(1))
            val futurePayment = createPayment(id = 2L, expiresAt = ZonedDateTime.now().plusMinutes(10))

            every { paymentRepository.findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(eq(PaymentStatus.PENDING), any()) } returns listOf(overduePayment)
            every { paymentRepository.save(overduePayment) } returns overduePayment

            val expiredCount = paymentService.expirePendingPayments(ZonedDateTime.now())

            assertThat(expiredCount).isEqualTo(1)
            assertThat(overduePayment.status).isEqualTo(PaymentStatus.EXPIRED)
            assertThat(futurePayment.status).isEqualTo(PaymentStatus.PENDING)
            verify(exactly = 1) { paymentRepository.save(overduePayment) }
        }
    }
}
