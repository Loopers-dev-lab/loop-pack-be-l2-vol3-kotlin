package com.loopers.domain.payment

import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentTest {

    private fun createPayment(
        orderId: Long = 1L,
        userId: Long = 1L,
        amount: Money = Money(50000),
        cardType: String = "SAMSUNG",
        cardNo: String = "1234-5678-9012-3456",
    ): Payment = Payment(
        orderId = orderId,
        userId = userId,
        amount = amount,
        cardType = cardType,
        cardNo = cardNo,
    )

    @Nested
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 결제를 생성하면 REQUESTED 상태로 생성된다")
        fun success() {
            // act
            val payment = createPayment()

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REQUESTED)
            assertThat(payment.transactionKey).isNull()
            assertThat(payment.failReason).isNull()
        }

        @Test
        @DisplayName("orderId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroOrderIdThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createPayment(orderId = 0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("userId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroUserIdThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createPayment(userId = 0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("금액이 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroAmountThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createPayment(amount = Money.ZERO)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("카드 종류가 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        fun blankCardTypeThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createPayment(cardType = "")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("카드 번호가 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        fun blankCardNoThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createPayment(cardNo = "")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class MarkApproved {

        @Test
        @DisplayName("REQUESTED 상태에서 APPROVED로 변경된다")
        fun fromRequestedSuccess() {
            // arrange
            val payment = createPayment()

            // act
            payment.markApproved("TXN-001")

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
            assertThat(payment.transactionKey).isEqualTo("TXN-001")
        }

        @Test
        @DisplayName("TIMEOUT 상태에서 APPROVED로 변경된다")
        fun fromTimeoutSuccess() {
            // arrange
            val payment = createPayment()
            payment.markTimeout()

            // act
            payment.markApproved("TXN-001")

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
        }

        @Test
        @DisplayName("이미 APPROVED 상태에서 호출하면 무시된다 (멱등성)")
        fun alreadyApprovedIsIdempotent() {
            // arrange
            val payment = createPayment()
            payment.markApproved("TXN-001")

            // act (재호출)
            payment.markApproved("TXN-001")

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
        }

        @Test
        @DisplayName("REJECTED 상태에서 호출하면 BAD_REQUEST 예외가 발생한다")
        fun fromRejectedThrowsBadRequest() {
            // arrange
            val payment = createPayment()
            payment.markRejected("한도 초과")

            // act
            val result = assertThrows<CoreException> {
                payment.markApproved("TXN-001")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class MarkRejected {

        @Test
        @DisplayName("REQUESTED 상태에서 REJECTED로 변경된다")
        fun fromRequestedSuccess() {
            // arrange
            val payment = createPayment()

            // act
            payment.markRejected("한도 초과")

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REJECTED)
            assertThat(payment.failReason).isEqualTo("한도 초과")
        }

        @Test
        @DisplayName("TIMEOUT 상태에서 REJECTED로 변경된다")
        fun fromTimeoutSuccess() {
            // arrange
            val payment = createPayment()
            payment.markTimeout()

            // act
            payment.markRejected("PG 요청 미도달")

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REJECTED)
        }

        @Test
        @DisplayName("APPROVED 상태에서 호출하면 BAD_REQUEST 예외가 발생한다")
        fun fromApprovedThrowsBadRequest() {
            // arrange
            val payment = createPayment()
            payment.markApproved("TXN-001")

            // act
            val result = assertThrows<CoreException> {
                payment.markRejected("취소")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class MarkTimeout {

        @Test
        @DisplayName("REQUESTED 상태에서 TIMEOUT으로 변경된다")
        fun fromRequestedSuccess() {
            // arrange
            val payment = createPayment()

            // act
            payment.markTimeout()

            // assert
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.TIMEOUT)
        }

        @Test
        @DisplayName("APPROVED 상태에서 호출하면 BAD_REQUEST 예외가 발생한다")
        fun fromApprovedThrowsBadRequest() {
            // arrange
            val payment = createPayment()
            payment.markApproved("TXN-001")

            // act
            val result = assertThrows<CoreException> {
                payment.markTimeout()
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class IsFinalized {

        @Test
        @DisplayName("APPROVED 상태이면 true를 반환한다")
        fun approvedIsFinalized() {
            val payment = createPayment()
            payment.markApproved("TXN-001")
            assertThat(payment.isFinalized()).isTrue()
        }

        @Test
        @DisplayName("REJECTED 상태이면 true를 반환한다")
        fun rejectedIsFinalized() {
            val payment = createPayment()
            payment.markRejected("한도 초과")
            assertThat(payment.isFinalized()).isTrue()
        }

        @Test
        @DisplayName("REQUESTED 상태이면 false를 반환한다")
        fun requestedIsNotFinalized() {
            val payment = createPayment()
            assertThat(payment.isFinalized()).isFalse()
        }

        @Test
        @DisplayName("TIMEOUT 상태이면 false를 반환한다")
        fun timeoutIsNotFinalized() {
            val payment = createPayment()
            payment.markTimeout()
            assertThat(payment.isFinalized()).isFalse()
        }
    }

    @Nested
    inner class UpdateTransactionKey {

        @Test
        @DisplayName("transactionKey가 업데이트된다")
        fun success() {
            // arrange
            val payment = createPayment()

            // act
            payment.updateTransactionKey("TXN-001")

            // assert
            assertThat(payment.transactionKey).isEqualTo("TXN-001")
        }
    }
}
