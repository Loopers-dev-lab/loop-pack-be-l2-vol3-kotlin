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

class PaymentTest {

    private fun createPayment() = Payment(
        orderId = 20260318000001L,
        userId = 1L,
        amount = BigDecimal("50000"),
        cardType = "SAMSUNG",
        cardNo = "****-****-****-1451",
    )

    @DisplayName("결제를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("정상적인 정보가 주어지면, INITIATED 상태로 생성된다.")
        @Test
        fun createsPayment_withInitiatedStatus() {
            // arrange & act
            val payment = createPayment()

            // assert
            assertAll(
                { assertThat(payment.orderId).isEqualTo(20260318000001L) },
                { assertThat(payment.userId).isEqualTo(1L) },
                { assertThat(payment.amount).isEqualByComparingTo(BigDecimal("50000")) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.INITIATED) },
                { assertThat(payment.transactionKey).isNull() },
                { assertThat(payment.failReason).isNull() },
            )
        }
    }

    @DisplayName("상태를 변경할 때,")
    @Nested
    inner class ChangeStatus {

        @DisplayName("INITIATED에서 markRequested()를 호출하면, REQUESTED로 변경되고 transactionKey가 저장된다.")
        @Test
        fun changesRequested_whenInitiated() {
            // arrange
            val payment = createPayment()

            // act
            payment.markRequested("20260318:TR:abc123")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(payment.transactionKey).isEqualTo("20260318:TR:abc123") },
            )
        }

        @DisplayName("REQUESTED에서 markPaid()를 호출하면, PAID로 변경된다.")
        @Test
        fun changesPaid_whenRequested() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")

            // act
            payment.markPaid()

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.PAID)
        }

        @DisplayName("REQUESTED에서 markFailed()를 호출하면, FAILED로 변경되고 실패 사유가 저장된다.")
        @Test
        fun changesFailed_whenRequested() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")

            // act
            payment.markFailed("한도초과입니다.")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failReason).isEqualTo("한도초과입니다.") },
            )
        }

        @DisplayName("INITIATED에서 markFailed()를 호출하면, FAILED로 변경된다.")
        @Test
        fun changesFailed_whenInitiated() {
            // arrange
            val payment = createPayment()

            // act
            payment.markFailed("PG 결제 요청 실패")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failReason).isEqualTo("PG 결제 요청 실패") },
            )
        }

        @DisplayName("REQUESTED에서 markRequested()를 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenAlreadyRequested() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")

            // act
            val exception = assertThrows<CoreException> {
                payment.markRequested("20260318:TR:def456")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("INITIATED에서 markPaid()를 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenPaidFromInitiated() {
            // arrange
            val payment = createPayment()

            // act
            val exception = assertThrows<CoreException> {
                payment.markPaid()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("PAID에서 markFailed()를 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenFailedFromPaid() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")
            payment.markPaid()

            // act
            val exception = assertThrows<CoreException> {
                payment.markFailed("테스트")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("카드 번호를 마스킹할 때,")
    @Nested
    inner class MaskCardNo {

        @DisplayName("xxxx-xxxx-xxxx-xxxx 형식이면, 마지막 4자리만 남긴다.")
        @Test
        fun masksCardNo_whenStandardFormat() {
            // act
            val masked = Payment.maskCardNo("1234-5678-9814-1451")

            // assert
            assertThat(masked).isEqualTo("****-****-****-1451")
        }

        @DisplayName("다른 형식이면, 마지막 4자리만 남긴다.")
        @Test
        fun masksCardNo_whenNonStandardFormat() {
            // act
            val masked = Payment.maskCardNo("1234567898141451")

            // assert
            assertThat(masked).isEqualTo("****1451")
        }
    }
}
