package com.loopers.domain.payment

import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentTest {

    @Nested
    @DisplayName("Payment.create 시")
    inner class Create {

        @Test
        @DisplayName("생성 직후 status는 REQUESTED이고 transactionKey는 null이다")
        fun create_initialState_requestedAndNullTransactionKey() {
            // act
            val payment = Payment.create(
                orderId = 1L,
                cardType = CardType.KB,
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
            )

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.REQUESTED)
            assertThat(payment.transactionKey).isNull()
            assertThat(payment.orderId).isEqualTo(1L)
            assertThat(payment.cardType).isEqualTo(CardType.KB)
            assertThat(payment.amount).isEqualTo(10000L)
        }

        @Test
        @DisplayName("cardNo는 마스킹되어 저장된다 — 첫 4자리와 마지막 4자리만 유지")
        fun create_cardNo_isMasked() {
            // act
            val payment = Payment.create(
                orderId = 1L,
                cardType = CardType.KB,
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
            )

            // assert
            assertThat(payment.cardNo).isEqualTo("1234-****-****-3456")
        }
    }

    @Nested
    @DisplayName("markSuccess 시")
    inner class MarkSuccess {

        @Test
        @DisplayName("REQUESTED 상태에서 호출하면 SUCCESS로 전이되고 transactionKey가 갱신된다")
        fun markSuccess_fromRequested_transitionToSuccess() {
            // arrange
            val payment = Payment.create(
                orderId = 1L,
                cardType = CardType.SAMSUNG,
                cardNo = "1111-2222-3333-4444",
                amount = 5000L,
            )

            // act
            payment.markSuccess("txn-key-001")

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(payment.transactionKey).isEqualTo("txn-key-001")
        }
    }

    @Nested
    @DisplayName("markFailed 시")
    inner class MarkFailed {

        @Test
        @DisplayName("REQUESTED 상태에서 호출하면 FAILED로 전이되고 reason이 설정된다")
        fun markFailed_fromRequested_transitionToFailed() {
            // arrange
            val payment = Payment.create(
                orderId = 2L,
                cardType = CardType.HYUNDAI,
                cardNo = "9999-8888-7777-6666",
                amount = 20000L,
            )

            // act
            payment.markFailed("한도 초과")

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.reason).isEqualTo("한도 초과")
        }
    }

    @Nested
    @DisplayName("markTimeout 시")
    inner class MarkTimeout {

        @Test
        @DisplayName("REQUESTED 상태에서 호출하면 TIMEOUT으로 전이된다")
        fun markTimeout_fromRequested_transitionToTimeout() {
            // arrange
            val payment = Payment.create(
                orderId = 3L,
                cardType = CardType.KB,
                cardNo = "1234-0000-0000-5678",
                amount = 15000L,
            )

            // act
            payment.markTimeout()

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.TIMEOUT)
        }
    }

    @Nested
    @DisplayName("TIMEOUT 상태 복구 시")
    inner class TimeoutRecovery {

        @Test
        @DisplayName("TIMEOUT 상태에서 markSuccess 호출 시 SUCCESS로 전이된다")
        fun markSuccess_fromTimeout_transitionToSuccess() {
            // arrange
            val payment = Payment.create(
                orderId = 4L,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-0000-0000",
                amount = 30000L,
            )
            payment.markTimeout()

            // act
            payment.markSuccess("txn-key-recovered")

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(payment.transactionKey).isEqualTo("txn-key-recovered")
        }

        @Test
        @DisplayName("TIMEOUT 상태에서 markFailed 호출 시 FAILED로 전이된다")
        fun markFailed_fromTimeout_transitionToFailed() {
            // arrange
            val payment = Payment.create(
                orderId = 5L,
                cardType = CardType.HYUNDAI,
                cardNo = "0000-1111-2222-3333",
                amount = 8000L,
            )
            payment.markTimeout()

            // act
            payment.markFailed("복구 실패")

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.reason).isEqualTo("복구 실패")
        }
    }

    @Nested
    @DisplayName("SUCCESS 상태에서 재전이 시")
    inner class SuccessReTransition {

        @Test
        @DisplayName("SUCCESS 상태에서 markSuccess 재호출 시 멱등하게 무시된다")
        fun markSuccess_fromSuccess_isIdempotent() {
            // arrange
            val payment = Payment.create(
                orderId = 6L,
                cardType = CardType.KB,
                cardNo = "5555-6666-7777-8888",
                amount = 12000L,
            )
            payment.markSuccess("txn-key-done")

            // act
            payment.markSuccess("txn-key-again")

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(payment.transactionKey).isEqualTo("txn-key-done")
        }

        @Test
        @DisplayName("SUCCESS 상태에서 markFailed 호출 시 CoreException이 발생한다")
        fun markFailed_fromSuccess_throwsCoreException() {
            // arrange
            val payment = Payment.create(
                orderId = 7L,
                cardType = CardType.SAMSUNG,
                cardNo = "1111-3333-5555-7777",
                amount = 25000L,
            )
            payment.markSuccess("txn-key-done")

            // act & assert
            assertThrows<CoreException> {
                payment.markFailed("실패 사유")
            }
        }

        @Test
        @DisplayName("SUCCESS 상태에서 markTimeout 호출 시 CoreException이 발생한다")
        fun markTimeout_fromSuccess_throwsCoreException() {
            // arrange
            val payment = Payment.create(
                orderId = 8L,
                cardType = CardType.HYUNDAI,
                cardNo = "2222-4444-6666-8888",
                amount = 7000L,
            )
            payment.markSuccess("txn-key-done")

            // act & assert
            assertThrows<CoreException> {
                payment.markTimeout()
            }
        }
    }

    @Nested
    @DisplayName("FAILED 상태에서 재전이 시")
    inner class FailedReTransition {

        @Test
        @DisplayName("FAILED 상태에서 markSuccess 호출 시 CoreException이 발생한다")
        fun markSuccess_fromFailed_throwsCoreException() {
            // arrange
            val payment = Payment.create(
                orderId = 9L,
                cardType = CardType.KB,
                cardNo = "9999-0000-1111-2222",
                amount = 18000L,
            )
            payment.markFailed("카드 오류")

            // act & assert
            assertThrows<CoreException> {
                payment.markSuccess("txn-key-retry")
            }
        }

        @Test
        @DisplayName("FAILED 상태에서 markFailed 재호출 시 멱등하게 무시된다")
        fun markFailed_fromFailed_isIdempotent() {
            // arrange
            val payment = Payment.create(
                orderId = 10L,
                cardType = CardType.SAMSUNG,
                cardNo = "3333-4444-5555-6666",
                amount = 9000L,
            )
            payment.markFailed("카드 오류")

            // act
            payment.markFailed("또 실패")

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.reason).isEqualTo("카드 오류")
        }

        @Test
        @DisplayName("FAILED 상태에서 markTimeout 호출 시 CoreException이 발생한다")
        fun markTimeout_fromFailed_throwsCoreException() {
            // arrange
            val payment = Payment.create(
                orderId = 11L,
                cardType = CardType.HYUNDAI,
                cardNo = "7777-8888-9999-0000",
                amount = 11000L,
            )
            payment.markFailed("카드 오류")

            // act & assert
            assertThrows<CoreException> {
                payment.markTimeout()
            }
        }
    }
}
