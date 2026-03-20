package com.loopers.domain.payment

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("Payment 도메인")
class PaymentTest {

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun createPayment(
        status: Payment.Status = Payment.Status.PENDING,
        transactionKey: String? = null,
        reasonCode: PaymentReasonCode? = null,
    ): Payment = if (status == Payment.Status.PENDING && transactionKey == null && reasonCode == null) {
        Payment.create(
            orderId = 1L,
            userId = 1L,
            idempotencyKey = PaymentIdempotencyKey("pay-key-001"),
            cardType = "VISA",
            maskedCardNo = "****1234",
            amount = Money(BigDecimal("10000")),
            requestFingerprint = "fingerprint-abc",
        )
    } else {
        Payment.retrieve(
            id = 100L,
            orderId = 1L,
            userId = 1L,
            idempotencyKey = PaymentIdempotencyKey("pay-key-001"),
            status = status,
            cardType = "VISA",
            maskedCardNo = "****1234",
            amount = Money(BigDecimal("10000")),
            transactionKey = transactionKey,
            reasonCode = reasonCode,
            requestFingerprint = "fingerprint-abc",
            createdAt = now,
        )
    }

    @Nested
    @DisplayName("생성")
    inner class Create {

        @Test
        @DisplayName("create 시 status는 PENDING, transactionKey는 null")
        fun create_initialState() {
            val payment = createPayment()

            assertAll(
                { assertThat(payment.id).isNull() },
                { assertThat(payment.status).isEqualTo(Payment.Status.PENDING) },
                { assertThat(payment.transactionKey).isNull() },
                { assertThat(payment.reasonCode).isNull() },
                { assertThat(payment.orderId).isEqualTo(1L) },
                { assertThat(payment.amount).isEqualTo(Money(BigDecimal("10000"))) },
            )
        }
    }

    @Nested
    @DisplayName("succeed — PENDING → SUCCESS 전이")
    inner class Succeed {

        @Test
        @DisplayName("PENDING에서 succeed → SUCCESS, transactionKey 설정")
        fun succeed_fromPending() {
            val payment = createPayment()

            val succeeded = payment.succeed("txn-key-001")

            assertAll(
                { assertThat(succeeded.status).isEqualTo(Payment.Status.SUCCESS) },
                { assertThat(succeeded.transactionKey).isEqualTo("txn-key-001") },
            )
        }

        @Test
        @DisplayName("FAILED에서 succeed → 예외")
        fun succeed_fromFailed() {
            val payment = createPayment(
                status = Payment.Status.FAILED,
                reasonCode = PaymentReasonCode.PG_INTERNAL_ERROR,
            )

            val exception = assertThrows<CoreException> {
                payment.succeed("txn-key-001")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
        }

        @Test
        @DisplayName("SUCCESS에서 succeed → 예외")
        fun succeed_fromSuccess() {
            val payment = createPayment(
                status = Payment.Status.SUCCESS,
                transactionKey = "txn-key-existing",
            )

            val exception = assertThrows<CoreException> {
                payment.succeed("txn-key-002")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
        }
    }

    @Nested
    @DisplayName("fail — PENDING → FAILED 전이")
    inner class Fail {

        @Test
        @DisplayName("PENDING에서 fail → FAILED, reasonCode 설정")
        fun fail_fromPending() {
            val payment = createPayment()

            val failed = payment.fail(PaymentReasonCode.PG_INTERNAL_ERROR)

            assertAll(
                { assertThat(failed.status).isEqualTo(Payment.Status.FAILED) },
                { assertThat(failed.reasonCode).isEqualTo(PaymentReasonCode.PG_INTERNAL_ERROR) },
            )
        }

        @Test
        @DisplayName("SUCCESS에서 fail → 예외")
        fun fail_fromSuccess() {
            val payment = createPayment(
                status = Payment.Status.SUCCESS,
                transactionKey = "txn-key-existing",
            )

            val exception = assertThrows<CoreException> {
                payment.fail(PaymentReasonCode.LIMIT_EXCEEDED)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
        }

        @Test
        @DisplayName("FAILED에서 fail → 예외")
        fun fail_fromFailed() {
            val payment = createPayment(
                status = Payment.Status.FAILED,
                reasonCode = PaymentReasonCode.PG_INTERNAL_ERROR,
            )

            val exception = assertThrows<CoreException> {
                payment.fail(PaymentReasonCode.INVALID_CARD)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_STATUS_TRANSITION)
        }
    }

    @Nested
    @DisplayName("isTerminal")
    inner class IsTerminal {

        @Test
        @DisplayName("PENDING은 terminal이 아니다")
        fun isTerminal_pending() {
            assertThat(createPayment().isTerminal).isFalse()
        }

        @Test
        @DisplayName("SUCCESS는 terminal이다")
        fun isTerminal_success() {
            assertThat(
                createPayment(status = Payment.Status.SUCCESS, transactionKey = "txn-1").isTerminal,
            ).isTrue()
        }

        @Test
        @DisplayName("FAILED는 terminal이다")
        fun isTerminal_failed() {
            assertThat(
                createPayment(
                    status = Payment.Status.FAILED,
                    reasonCode = PaymentReasonCode.PG_INTERNAL_ERROR,
                ).isTerminal,
            ).isTrue()
        }
    }
}
