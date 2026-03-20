package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("PaymentModel")
class PaymentModelTest {

    companion object {
        private const val ORDER_ID = 1L
        private const val PAYMENT_AMOUNT = 50_000L
    }

    private fun createPayment(
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusMinutes(30),
    ): PaymentModel {
        return PaymentModel(
            orderId = ORDER_ID,
            amount = PAYMENT_AMOUNT,
            expiresAt = expiresAt,
        )
    }

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("결제를 생성하면 초기 상태는 PENDING 이다")
        @Test
        fun createsPayment_withPendingStatus() {
            val expiresAt = ZonedDateTime.now().plusMinutes(30)
            val payment = createPayment(expiresAt)

            assertThat(payment.orderId).isEqualTo(ORDER_ID)
            assertThat(payment.amount).isEqualTo(PAYMENT_AMOUNT)
            assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
            assertThat(payment.externalTransactionId).isNull()
            assertThat(payment.failureReason).isNull()
            assertThat(payment.expiresAt).isEqualTo(expiresAt)
        }

        @DisplayName("신규 결제는 항상 PENDING 상태로 시작한다")
        @Test
        fun createsPayment_alwaysWithPendingStatus() {
            val payment = PaymentModel(
                orderId = ORDER_ID,
                amount = PAYMENT_AMOUNT,
                expiresAt = ZonedDateTime.now().plusMinutes(10),
            )

            assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        }
    }

    @DisplayName("markSucceeded")
    @Nested
    inner class MarkSucceeded {
        @DisplayName("PENDING 상태의 결제는 성공 처리할 수 있다")
        @Test
        fun marksPaymentAsSucceeded_whenStatusIsPending() {
            val payment = createPayment()
            val transactionId = "tx-001"

            payment.markSucceeded(transactionId)

            assertThat(payment.status).isEqualTo(PaymentStatus.SUCCEEDED)
            assertThat(payment.externalTransactionId).isEqualTo(transactionId)
            assertThat(payment.failureReason).isNull()
        }

        @DisplayName("FAILED 상태의 결제는 성공 처리할 수 없다")
        @Test
        fun throwsBadRequest_whenPaymentAlreadyFailed() {
            val payment = createPayment()
            payment.markFailed("pg error")

            assertThatThrownBy {
                payment.markSucceeded("tx-001")
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("성공 처리할 수 없습니다")
        }

        @DisplayName("EXPIRED 상태의 결제는 성공 처리할 수 없다")
        @Test
        fun throwsBadRequest_whenPaymentAlreadyExpired() {
            val payment = createPayment()
            payment.markExpired()

            assertThatThrownBy {
                payment.markSucceeded("tx-001")
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("성공 처리할 수 없습니다")
        }

        @DisplayName("SUCCEEDED 상태의 결제는 다시 성공 처리할 수 없다")
        @Test
        fun throwsBadRequest_whenPaymentAlreadySucceeded() {
            val payment = createPayment()
            payment.markSucceeded("tx-001")

            assertThatThrownBy {
                payment.markSucceeded("tx-002")
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("성공 처리할 수 없습니다")
        }
    }

    @DisplayName("markFailed")
    @Nested
    inner class MarkFailed {
        @DisplayName("PENDING 상태의 결제는 실패 처리할 수 있다")
        @Test
        fun marksPaymentAsFailed_whenStatusIsPending() {
            val payment = createPayment()
            val failureReason = "pg error"

            payment.markFailed(failureReason)

            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.failureReason).isEqualTo(failureReason)
            assertThat(payment.externalTransactionId).isNull()
        }
    }

    @DisplayName("markExpired")
    @Nested
    inner class MarkExpired {
        @DisplayName("PENDING 상태의 결제는 만료 처리할 수 있다")
        @Test
        fun marksPaymentAsExpired_whenStatusIsPending() {
            val payment = createPayment()

            payment.markExpired()

            assertThat(payment.status).isEqualTo(PaymentStatus.EXPIRED)
            assertThat(payment.externalTransactionId).isNull()
        }
    }
}
