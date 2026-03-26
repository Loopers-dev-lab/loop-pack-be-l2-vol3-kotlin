package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class PaymentServiceIntegrationTest @Autowired constructor(
    private val paymentService: PaymentService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_ORDER_ID = 1L
        private const val DEFAULT_USER_ID = 1L
        private val DEFAULT_AMOUNT = BigDecimal("50000")
        private const val DEFAULT_CARD_TYPE = "VISA"
        private const val DEFAULT_CARD_NO = "4111111111111111"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createPaymentCommand(
        orderId: Long = DEFAULT_ORDER_ID,
        userId: Long = DEFAULT_USER_ID,
        amount: BigDecimal = DEFAULT_AMOUNT,
        cardType: String = DEFAULT_CARD_TYPE,
        cardNo: String = DEFAULT_CARD_NO,
    ) = CreatePaymentCommand(
        orderId = orderId,
        userId = userId,
        amount = amount,
        cardType = cardType,
        cardNo = cardNo,
    )

    @DisplayName("결제 생성")
    @Nested
    inner class CreatePayment {
        @DisplayName("유효한 정보가 주어지면, PENDING 상태로 생성된다.")
        @Test
        fun createsPaymentWithPendingStatusWhenValidInfoIsProvided() {
            // arrange
            val command = createPaymentCommand()

            // act
            val result = paymentService.createPayment(command)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.orderId).isEqualTo(DEFAULT_ORDER_ID) },
                { assertThat(result.userId).isEqualTo(DEFAULT_USER_ID) },
                { assertThat(result.amount).isEqualByComparingTo(DEFAULT_AMOUNT) },
                { assertThat(result.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(result.cardType).isEqualTo(DEFAULT_CARD_TYPE) },
                { assertThat(result.cardNo).isEqualTo(DEFAULT_CARD_NO) },
            )
        }

        @DisplayName("이미 SUCCESS 결제가 존재하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenSuccessPaymentAlreadyExists() {
            // arrange
            val first = paymentService.createPayment(createPaymentCommand())
            paymentService.updateTransactionKey(first.id, "txn-1")
            paymentService.completePayment(CompletePaymentCommand(transactionKey = "txn-1"))

            // act & assert
            val result = assertThrows<CoreException> {
                paymentService.createPayment(createPaymentCommand())
            }
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("기존 PENDING 결제가 있으면, FAILED 처리 후 새 결제가 생성된다.")
        @Test
        fun failsExistingPendingAndCreatesNewPayment() {
            // arrange
            val first = paymentService.createPayment(createPaymentCommand())

            // act
            val second = paymentService.createPayment(createPaymentCommand())

            // assert
            val firstPayment = paymentService.getPayment(first.id)
            assertAll(
                { assertThat(firstPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(second.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(second.id).isNotEqualTo(first.id) },
            )
        }
    }

    @DisplayName("결제 완료 처리")
    @Nested
    inner class CompletePayment {
        @DisplayName("transactionKey로 조회하여 SUCCESS로 전이된다.")
        @Test
        fun transitionsToSuccessWhenValidTransactionKeyIsProvided() {
            // arrange
            val payment = paymentService.createPayment(createPaymentCommand())
            val expectedTransactionKey = "txn-success-1"
            paymentService.updateTransactionKey(payment.id, expectedTransactionKey)

            // act
            val result = paymentService.completePayment(CompletePaymentCommand(transactionKey = expectedTransactionKey))

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(result.transactionKey).isEqualTo(expectedTransactionKey) },
            )
        }

        @DisplayName("존재하지 않는 transactionKey이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenTransactionKeyDoesNotExist() {
            // act & assert
            val result = assertThrows<CoreException> {
                paymentService.completePayment(CompletePaymentCommand(transactionKey = "non-existent"))
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("결제 실패 처리")
    @Nested
    inner class FailPayment {
        @DisplayName("transactionKey로 조회하여 FAILED로 전이된다.")
        @Test
        fun transitionsToFailedWhenValidTransactionKeyIsProvided() {
            // arrange
            val payment = paymentService.createPayment(createPaymentCommand())
            val expectedTransactionKey = "txn-fail-1"
            val expectedReason = "잔액 부족"
            paymentService.updateTransactionKey(payment.id, expectedTransactionKey)

            // act
            val result = paymentService.failPayment(
                FailPaymentCommand(transactionKey = expectedTransactionKey, reason = expectedReason),
            )

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(result.failReason).isEqualTo(expectedReason) },
            )
        }
    }

    @DisplayName("transactionKey 업데이트")
    @Nested
    inner class UpdateTransactionKey {
        @DisplayName("유효한 결제에 transactionKey를 설정한다.")
        @Test
        fun updatesTransactionKeySuccessfully() {
            // arrange
            val payment = paymentService.createPayment(createPaymentCommand())
            val expectedTransactionKey = "txn-update-1"

            // act
            val result = paymentService.updateTransactionKey(payment.id, expectedTransactionKey)

            // assert
            assertThat(result.transactionKey).isEqualTo(expectedTransactionKey)
        }

        @DisplayName("존재하지 않는 결제이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenPaymentDoesNotExist() {
            // act & assert
            val result = assertThrows<CoreException> {
                paymentService.updateTransactionKey(999L, "txn-1")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
