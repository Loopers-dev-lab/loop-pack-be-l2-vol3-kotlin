package com.loopers.application.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentService
import com.loopers.infrastructure.payment.PgClient
import com.loopers.infrastructure.payment.PgPaymentRequest
import com.loopers.infrastructure.payment.PgPaymentResponse
import com.loopers.infrastructure.payment.PgPaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("PaymentFacade")
class PaymentFacadeTest {

    private val paymentService: PaymentService = mockk()
    private val pgClient: PgClient = mockk()
    private val paymentFacade = PaymentFacade(paymentService, pgClient)

    companion object {
        private const val PAYMENT_ID = 1L
        private const val ORDER_ID = 100L
        private const val PAYMENT_AMOUNT = 50_000L
    }

    private fun createPayment(): PaymentModel {
        val payment = PaymentModel(
            orderId = ORDER_ID,
            amount = PAYMENT_AMOUNT,
            expiresAt = ZonedDateTime.now().plusMinutes(30),
        )
        return spyk(payment) {
            every { this@spyk.id } returns PAYMENT_ID
        }
    }

    @DisplayName("requestPayment")
    @Nested
    inner class RequestPayment {
        @DisplayName("PG 승인에 성공하면 결제를 성공 처리한다")
        @Test
        fun marksPaymentSucceeded_whenPgApproves() {
            val payment = createPayment()
            every { paymentService.createPayment(eq(ORDER_ID), eq(PAYMENT_AMOUNT), any()) } returns payment
            every {
                pgClient.requestPayment(PgPaymentRequest(orderId = ORDER_ID, amount = PAYMENT_AMOUNT))
            } returns PgPaymentResponse(
                orderId = ORDER_ID,
                amount = PAYMENT_AMOUNT,
                transactionId = "pg-100",
                status = PgPaymentStatus.APPROVED,
            )
            every { paymentService.markSucceeded(PAYMENT_ID, "pg-100") } returns payment

            paymentFacade.requestPayment(orderId = ORDER_ID, amount = PAYMENT_AMOUNT)

            verify(exactly = 1) { paymentService.createPayment(eq(ORDER_ID), eq(PAYMENT_AMOUNT), any()) }
            verify(exactly = 1) { paymentService.markSucceeded(PAYMENT_ID, "pg-100") }
        }

        @DisplayName("PG 요청에 실패하면 결제를 실패 처리하고 예외를 다시 던진다")
        @Test
        fun marksPaymentFailedAndRethrows_whenPgDeclines() {
            val payment = createPayment()
            val failure = CoreException(ErrorType.CONFLICT, "카드 승인에 실패했습니다.")
            every { paymentService.createPayment(eq(ORDER_ID), eq(PAYMENT_AMOUNT), any()) } returns payment
            every {
                pgClient.requestPayment(PgPaymentRequest(orderId = ORDER_ID, amount = PAYMENT_AMOUNT))
            } throws failure
            every { paymentService.markFailed(PAYMENT_ID, "카드 승인에 실패했습니다.") } answers {
                payment.markFailed(secondArg())
                payment
            }

            assertThatThrownBy {
                paymentFacade.requestPayment(orderId = ORDER_ID, amount = PAYMENT_AMOUNT)
            }
                .isSameAs(failure)

            verify(exactly = 1) { paymentService.markFailed(PAYMENT_ID, "카드 승인에 실패했습니다.") }
        }

        @DisplayName("PG 승인 후 내부 성공 처리에 실패하면 실패 결제로 덮어쓰지 않고 예외를 전파한다")
        @Test
        fun rethrowsWithoutMarkingFailed_whenMarkSucceededFailsAfterApproval() {
            val payment = createPayment()
            val markSucceededFailure = CoreException(ErrorType.INTERNAL_ERROR, "결제 상태 저장에 실패했습니다.")
            every { paymentService.createPayment(eq(ORDER_ID), eq(PAYMENT_AMOUNT), any()) } returns payment
            every {
                pgClient.requestPayment(PgPaymentRequest(orderId = ORDER_ID, amount = PAYMENT_AMOUNT))
            } returns PgPaymentResponse(
                orderId = ORDER_ID,
                amount = PAYMENT_AMOUNT,
                transactionId = "pg-100",
                status = PgPaymentStatus.APPROVED,
            )
            every { paymentService.markSucceeded(PAYMENT_ID, "pg-100") } throws markSucceededFailure

            assertThatThrownBy {
                paymentFacade.requestPayment(orderId = ORDER_ID, amount = PAYMENT_AMOUNT)
            }
                .isSameAs(markSucceededFailure)

            verify(exactly = 0) { paymentService.markFailed(any(), any()) }
        }
    }
}
