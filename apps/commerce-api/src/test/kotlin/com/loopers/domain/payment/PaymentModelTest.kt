package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PaymentModelTest {
    companion object {
        private const val DEFAULT_ORDER_ID = 1L
        private const val DEFAULT_USER_ID = 1L
        private val DEFAULT_AMOUNT = BigDecimal("50000")
        private const val DEFAULT_CARD_TYPE = "VISA"
        private const val DEFAULT_CARD_NO = "4111111111111111"
    }

    private fun createPaymentModel(
        orderId: Long = DEFAULT_ORDER_ID,
        userId: Long = DEFAULT_USER_ID,
        amount: BigDecimal = DEFAULT_AMOUNT,
        cardType: String = DEFAULT_CARD_TYPE,
        cardNo: String = DEFAULT_CARD_NO,
    ) = PaymentModel(
        orderId = orderId,
        userId = userId,
        amount = amount,
        cardType = cardType,
        cardNo = cardNo,
    )

    @DisplayName("생성")
    @Nested
    inner class Create {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsPaymentModelWhenValidParametersAreProvided() {
            // act
            val payment = createPaymentModel()

            // assert
            assertAll(
                { assertThat(payment.orderId).isEqualTo(DEFAULT_ORDER_ID) },
                { assertThat(payment.userId).isEqualTo(DEFAULT_USER_ID) },
                { assertThat(payment.amount).isEqualByComparingTo(DEFAULT_AMOUNT) },
                { assertThat(payment.cardType).isEqualTo(DEFAULT_CARD_TYPE) },
                { assertThat(payment.cardNo).isEqualTo(DEFAULT_CARD_NO) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(payment.transactionKey).isNull() },
                { assertThat(payment.failReason).isNull() },
            )
        }

        @DisplayName("금액이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenAmountIsZeroOrNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createPaymentModel(amount = BigDecimal.ZERO)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("카드 종류가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenCardTypeIsBlank() {
            // act & assert
            val result = assertThrows<CoreException> {
                createPaymentModel(cardType = "   ")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("카드 번호가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenCardNoIsBlank() {
            // act & assert
            val result = assertThrows<CoreException> {
                createPaymentModel(cardNo = "   ")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("transactionKey 업데이트")
    @Nested
    inner class UpdateTransactionKey {
        @DisplayName("transactionKey를 정상적으로 업데이트한다.")
        @Test
        fun updatesTransactionKeySuccessfully() {
            // arrange
            val payment = createPaymentModel()
            val expectedTransactionKey = "txn-12345"

            // act
            payment.updateTransactionKey(expectedTransactionKey)

            // assert
            assertThat(payment.transactionKey).isEqualTo(expectedTransactionKey)
        }
    }

    @DisplayName("결제 성공 처리")
    @Nested
    inner class MarkSuccess {
        @DisplayName("PENDING 상태에서 SUCCESS로 전이된다.")
        @Test
        fun transitionsFromPendingToSuccess() {
            // arrange
            val payment = createPaymentModel()

            // act
            payment.markSuccess()

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("SUCCESS 상태에서 다시 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenAlreadySuccess() {
            // arrange
            val payment = createPaymentModel()
            payment.markSuccess()

            // act & assert
            val result = assertThrows<CoreException> {
                payment.markSuccess()
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("FAILED 상태에서 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenAlreadyFailed() {
            // arrange
            val payment = createPaymentModel()
            payment.markFailed("테스트 실패")

            // act & assert
            val result = assertThrows<CoreException> {
                payment.markSuccess()
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제 실패 처리")
    @Nested
    inner class MarkFailed {
        @DisplayName("PENDING 상태에서 FAILED로 전이된다.")
        @Test
        fun transitionsFromPendingToFailed() {
            // arrange
            val payment = createPaymentModel()
            val expectedReason = "잔액 부족"

            // act
            payment.markFailed(expectedReason)

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failReason).isEqualTo(expectedReason) },
            )
        }

        @DisplayName("실패 사유 없이도 FAILED로 전이된다.")
        @Test
        fun transitionsFromPendingToFailedWithoutReason() {
            // arrange
            val payment = createPaymentModel()

            // act
            payment.markFailed()

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failReason).isNull() },
            )
        }

        @DisplayName("SUCCESS 상태에서 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenAlreadySuccess() {
            // arrange
            val payment = createPaymentModel()
            payment.markSuccess()

            // act & assert
            val result = assertThrows<CoreException> {
                payment.markFailed("실패")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("FAILED 상태에서 다시 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenAlreadyFailed() {
            // arrange
            val payment = createPaymentModel()
            payment.markFailed("첫 번째 실패")

            // act & assert
            val result = assertThrows<CoreException> {
                payment.markFailed("두 번째 실패")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
