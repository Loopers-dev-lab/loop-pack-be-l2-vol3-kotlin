package com.loopers.domain.payment

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class PaymentModelTest {
    private fun createPayment(
        status: PaymentStatus = PaymentStatus.REQUESTED,
        transactionKey: String? = null,
    ) = PaymentModel(
        orderId = 1L,
        memberId = 1L,
        cardType = CardType.SAMSUNG,
        cardNo = "1234-5678-9012-3456",
        amount = 50000,
        status = status,
        transactionKey = transactionKey,
    )

    @DisplayName("결제 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("모든 값이 유효하면, 정상적으로 생성된다.")
        @Test
        fun createsPayment_whenAllFieldsAreValid() {
            // act
            val payment = createPayment()

            // assert
            assertAll(
                { assertThat(payment.orderId).isEqualTo(1L) },
                { assertThat(payment.memberId).isEqualTo(1L) },
                { assertThat(payment.cardType).isEqualTo(CardType.SAMSUNG) },
                { assertThat(payment.amount).isEqualTo(50000) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(payment.transactionKey).isNull() },
            )
        }
    }

    @DisplayName("transactionKey를 할당할 때,")
    @Nested
    inner class AssignTransactionKey {
        @DisplayName("REQUESTED 상태이면, PENDING으로 전이되고 transactionKey가 설정된다.")
        @Test
        fun assignsKey_whenRequested() {
            // arrange
            val payment = createPayment(status = PaymentStatus.REQUESTED)

            // act
            val updated = payment.assignTransactionKey("txn-123")

            // assert
            assertAll(
                { assertThat(updated.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(updated.transactionKey).isEqualTo("txn-123") },
            )
        }

        @DisplayName("REQUESTED 상태가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNotRequested() {
            // arrange
            val payment = createPayment(status = PaymentStatus.PENDING, transactionKey = "txn-123")

            // act & assert
            val result = assertThrows<CoreException> { payment.assignTransactionKey("txn-456") }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제를 성공 처리할 때,")
    @Nested
    inner class MarkSuccess {
        @DisplayName("PENDING 상태이면, SUCCESS로 전이된다.")
        @Test
        fun marksSuccess_whenPending() {
            // arrange
            val payment = createPayment(status = PaymentStatus.PENDING, transactionKey = "txn-123")

            // act
            val updated = payment.markSuccess()

            // assert
            assertAll(
                { assertThat(updated.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(updated.completedAt).isNotNull() },
            )
        }

        @DisplayName("PENDING 상태가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNotPending() {
            // arrange
            val payment = createPayment(status = PaymentStatus.REQUESTED)

            // act & assert
            val result = assertThrows<CoreException> { payment.markSuccess() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제를 실패 처리할 때,")
    @Nested
    inner class MarkFailed {
        @DisplayName("REQUESTED 상태이면, FAILED로 전이된다.")
        @Test
        fun marksFailed_whenRequested() {
            // arrange
            val payment = createPayment(status = PaymentStatus.REQUESTED)

            // act
            val updated = payment.markFailed("PG 요청 실패")

            // assert
            assertAll(
                { assertThat(updated.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(updated.failReason).isEqualTo("PG 요청 실패") },
                { assertThat(updated.completedAt).isNotNull() },
            )
        }

        @DisplayName("PENDING 상태이면, FAILED로 전이된다.")
        @Test
        fun marksFailed_whenPending() {
            // arrange
            val payment = createPayment(status = PaymentStatus.PENDING, transactionKey = "txn-123")

            // act
            val updated = payment.markFailed("결제 실패")

            // assert
            assertThat(updated.status).isEqualTo(PaymentStatus.FAILED)
        }

        @DisplayName("이미 SUCCESS 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAlreadySuccess() {
            // arrange
            val payment = createPayment(status = PaymentStatus.PENDING, transactionKey = "txn-123")
                .markSuccess()

            // act & assert
            val result = assertThrows<CoreException> { payment.markFailed("실패") }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("종료 상태를 확인할 때,")
    @Nested
    inner class IsTerminal {
        @DisplayName("SUCCESS이면, true를 반환한다.")
        @Test
        fun returnsTrue_whenSuccess() {
            val payment = createPayment(status = PaymentStatus.PENDING, transactionKey = "txn-123")
                .markSuccess()
            assertThat(payment.isTerminal()).isTrue()
        }

        @DisplayName("FAILED이면, true를 반환한다.")
        @Test
        fun returnsTrue_whenFailed() {
            val payment = createPayment(status = PaymentStatus.REQUESTED)
                .markFailed("실패")
            assertThat(payment.isTerminal()).isTrue()
        }

        @DisplayName("REQUESTED이면, false를 반환한다.")
        @Test
        fun returnsFalse_whenRequested() {
            val payment = createPayment(status = PaymentStatus.REQUESTED)
            assertThat(payment.isTerminal()).isFalse()
        }
    }
}
