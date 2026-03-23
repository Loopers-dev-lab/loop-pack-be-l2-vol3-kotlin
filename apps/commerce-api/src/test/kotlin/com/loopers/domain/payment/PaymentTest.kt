package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class PaymentTest {

    private fun createPayment(
        userId: Long = 1L,
        orderId: String = "ORDER-001",
        cardType: CardType = CardType.SAMSUNG,
        cardNo: String = "1234-5678-9012-3456",
        amount: Long = 50000L,
    ): Payment = Payment(
        userId = userId,
        orderId = orderId,
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
    )

    @DisplayName("결제를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 값이 주어지면, REQUESTED 상태로 생성된다.")
        @Test
        fun createsPayment_whenValidValuesProvided() {
            // arrange & act
            val payment = createPayment()

            // assert
            assertAll(
                { assertThat(payment.userId).isEqualTo(1L) },
                { assertThat(payment.orderId).isEqualTo("ORDER-001") },
                { assertThat(payment.cardType).isEqualTo(CardType.SAMSUNG) },
                { assertThat(payment.cardNo).isEqualTo("1234-5678-9012-3456") },
                { assertThat(payment.amount).isEqualTo(50000L) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(payment.transactionKey).isNull() },
            )
        }

        @DisplayName("금액이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAmountIsZeroOrNegative() {
            // act
            val exception = assertThrows<CoreException> {
                createPayment(amount = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("orderId가 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenOrderIdIsBlank() {
            // act
            val exception = assertThrows<CoreException> {
                createPayment(orderId = "")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("cardNo가 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCardNoIsBlank() {
            // act
            val exception = assertThrows<CoreException> {
                createPayment(cardNo = "")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("PG 응답을 반영할 때,")
    @Nested
    inner class MarkPending {

        @DisplayName("transactionKey를 설정하고 PENDING 상태로 변경된다.")
        @Test
        fun setsTransactionKeyAndChangeStatusToPending() {
            // arrange
            val payment = createPayment()

            // act
            payment.markPending("txn-key-12345")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(payment.transactionKey).isEqualTo("txn-key-12345") },
            )
        }

        @DisplayName("이미 PENDING 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAlreadyPending() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-12345")

            // act
            val exception = assertThrows<CoreException> {
                payment.markPending("txn-key-99999")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제를 승인할 때,")
    @Nested
    inner class MarkSuccess {

        @DisplayName("PENDING 상태에서 SUCCESS로 변경된다.")
        @Test
        fun changesStatusToSuccess_whenPending() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-12345")

            // act
            payment.markSuccess()

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("REQUESTED 상태에서는 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenRequested() {
            // arrange
            val payment = createPayment()

            // act
            val exception = assertThrows<CoreException> {
                payment.markSuccess()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제를 실패 처리할 때,")
    @Nested
    inner class MarkFailed {

        @DisplayName("PENDING 상태에서 FAILED로 변경되고 사유가 저장된다.")
        @Test
        fun changesStatusToFailed_whenPending() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-12345")

            // act
            payment.markFailed("한도 초과")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failReason).isEqualTo("한도 초과") },
            )
        }

        @DisplayName("REQUESTED 상태에서 FAILED로 변경될 수 있다.")
        @Test
        fun changesStatusToFailed_whenRequested() {
            // arrange
            val payment = createPayment()

            // act
            payment.markFailed("PG 연결 실패")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failReason).isEqualTo("PG 연결 실패") },
            )
        }

        @DisplayName("SUCCESS 상태에서는 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAlreadySuccess() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-12345")
            payment.markSuccess()

            // act
            val exception = assertThrows<CoreException> {
                payment.markFailed("실패 사유")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
