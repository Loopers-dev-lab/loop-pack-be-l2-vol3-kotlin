package com.loopers.domain.payment

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentServiceTest {

    private lateinit var paymentService: PaymentService
    private lateinit var fakePaymentRepository: FakePaymentRepository

    @BeforeEach
    fun setUp() {
        fakePaymentRepository = FakePaymentRepository()
        paymentService = PaymentService(fakePaymentRepository)
    }

    private fun createPaymentCommand(
        orderId: Long = 1L,
        userId: Long = 1L,
        amount: Money = Money(50000),
        cardType: String = "SAMSUNG",
        cardNo: String = "1234-5678-9012-3456",
    ): CreatePaymentCommand = CreatePaymentCommand(
        orderId = orderId,
        userId = userId,
        amount = amount,
        cardType = cardType,
        cardNo = cardNo,
    )

    private fun createAndSavePayment(
        orderId: Long = 1L,
        userId: Long = 1L,
    ): Payment {
        return paymentService.createPayment(createPaymentCommand(orderId = orderId, userId = userId))
    }

    @Nested
    inner class CreatePayment {

        @Test
        @DisplayName("결제가 REQUESTED 상태로 생성된다")
        fun success() {
            // act
            val payment = paymentService.createPayment(createPaymentCommand())

            // assert
            assertThat(payment.id).isGreaterThan(0L)
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REQUESTED)
            assertThat(payment.orderId).isEqualTo(1L)
        }
    }

    @Nested
    inner class SyncPaymentStatus {

        @Test
        @DisplayName("PG 상태가 SUCCESS이면 Payment가 APPROVED로 변경된다")
        fun successToApproved() {
            // arrange
            val payment = createAndSavePayment()
            paymentService.updateTransactionKey(payment.id, "TXN-001")

            // act
            val result = paymentService.syncPaymentStatus(payment.id, "SUCCESS", null)

            // assert
            val found = paymentService.findById(payment.id)
            assertThat(result).isEqualTo("SUCCESS")
            assertThat(found.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
        }

        @Test
        @DisplayName("SUCCESS 상태에서 transactionKey가 없으면 BAD_REQUEST 예외가 발생한다")
        fun successWithNullTransactionKeyThrowsBadRequest() {
            // arrange
            val payment = createAndSavePayment()
            // transactionKey를 저장하지 않음 (null 상태)

            // act
            val result = assertThrows<CoreException> {
                paymentService.syncPaymentStatus(payment.id, "SUCCESS", null)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("PG 상태가 FAILED이면 Payment가 REJECTED로 변경된다")
        fun failedToRejected() {
            // arrange
            val payment = createAndSavePayment()

            // act
            val result = paymentService.syncPaymentStatus(payment.id, "FAILED", "한도 초과")

            // assert
            val found = paymentService.findById(payment.id)
            assertThat(result).isEqualTo("FAILED")
            assertThat(found.paymentStatus).isEqualTo(PaymentStatus.REJECTED)
            assertThat(found.failReason).isEqualTo("한도 초과")
        }

        @Test
        @DisplayName("PG 상태가 PENDING이면 Payment 상태가 변경되지 않는다")
        fun pendingNoChange() {
            // arrange
            val payment = createAndSavePayment()

            // act
            val result = paymentService.syncPaymentStatus(payment.id, "PENDING", null)

            // assert
            val found = paymentService.findById(payment.id)
            assertThat(result).isEqualTo("PENDING")
            assertThat(found.paymentStatus).isEqualTo(PaymentStatus.REQUESTED)
        }

        @Test
        @DisplayName("이미 APPROVED 상태이면 중복 처리되지 않는다 (멱등성)")
        fun alreadyApprovedIsIdempotent() {
            // arrange
            val payment = createAndSavePayment()
            paymentService.updateTransactionKey(payment.id, "TXN-001")
            paymentService.syncPaymentStatus(payment.id, "SUCCESS", null)

            // act (재호출)
            val result = paymentService.syncPaymentStatus(payment.id, "SUCCESS", null)

            // assert
            val found = paymentService.findById(payment.id)
            assertThat(result).isEqualTo("SUCCESS")
            assertThat(found.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
        }

        @Test
        @DisplayName("이미 REJECTED 상태이면 중복 처리되지 않는다 (멱등성)")
        fun alreadyRejectedIsIdempotent() {
            // arrange
            val payment = createAndSavePayment()
            paymentService.syncPaymentStatus(payment.id, "FAILED", "한도 초과")

            // act (재호출)
            val result = paymentService.syncPaymentStatus(payment.id, "FAILED", "다른 사유")

            // assert
            val found = paymentService.findById(payment.id)
            assertThat(result).isEqualTo("FAILED")
            assertThat(found.paymentStatus).isEqualTo(PaymentStatus.REJECTED)
            assertThat(found.failReason).isEqualTo("한도 초과") // 원래 사유 유지
        }
    }

    @Nested
    inner class FindById {

        @Test
        @DisplayName("존재하지 않는 결제를 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsException() {
            val result = assertThrows<CoreException> {
                paymentService.findById(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class FindByTransactionKey {

        @Test
        @DisplayName("transactionKey로 결제를 조회할 수 있다")
        fun success() {
            // arrange
            val payment = createAndSavePayment()
            paymentService.updateTransactionKey(payment.id, "TXN-001")

            // act
            val found = paymentService.findByTransactionKey("TXN-001")

            // assert
            assertThat(found.id).isEqualTo(payment.id)
        }

        @Test
        @DisplayName("존재하지 않는 transactionKey로 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsException() {
            val result = assertThrows<CoreException> {
                paymentService.findByTransactionKey("INVALID")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class FindPendingPayments {

        @Test
        @DisplayName("REQUESTED, TIMEOUT 상태의 결제만 조회된다")
        fun success() {
            // arrange
            val requested = createAndSavePayment(orderId = 1L)
            val timeout = createAndSavePayment(orderId = 2L)
            paymentService.markTimeout(timeout.id)
            val approved = createAndSavePayment(orderId = 3L)
            paymentService.updateTransactionKey(approved.id, "TXN-003")
            paymentService.markApproved(approved.id, "TXN-003")

            // act
            val pendingPayments = paymentService.findPendingPayments()

            // assert
            assertThat(pendingPayments).hasSize(2)
            assertThat(pendingPayments.map { it.paymentStatus })
                .containsExactlyInAnyOrder(PaymentStatus.REQUESTED, PaymentStatus.TIMEOUT)
        }
    }

    @Nested
    inner class MarkTimeout {

        @Test
        @DisplayName("REQUESTED 상태에서 TIMEOUT으로 변경된다")
        fun success() {
            // arrange
            val payment = createAndSavePayment()

            // act
            paymentService.markTimeout(payment.id)

            // assert
            val found = paymentService.findById(payment.id)
            assertThat(found.paymentStatus).isEqualTo(PaymentStatus.TIMEOUT)
        }
    }
}
