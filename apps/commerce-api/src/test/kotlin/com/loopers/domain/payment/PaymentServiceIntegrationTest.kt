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

@SpringBootTest
class PaymentServiceIntegrationTest @Autowired constructor(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createPayment(
        userId: Long = 1L,
        orderId: String = "ORDER-001",
        cardType: CardType = CardType.SAMSUNG,
        cardNo: String = "1234-5678-9012-3456",
        amount: Long = 50000L,
    ): Payment {
        return paymentRepository.save(
            Payment(
                userId = userId,
                orderId = orderId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
            ),
        )
    }

    @DisplayName("결제를 생성할 때,")
    @Nested
    inner class CreatePayment {

        @DisplayName("Payment가 REQUESTED 상태로 DB에 저장된다.")
        @Test
        fun savesPaymentToDatabase() {
            // act
            val payment = paymentService.createPayment(
                userId = 1L,
                orderId = "ORDER-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // assert
            assertAll(
                { assertThat(payment.id).isNotEqualTo(0L) },
                { assertThat(payment.userId).isEqualTo(1L) },
                { assertThat(payment.orderId).isEqualTo("ORDER-001") },
                { assertThat(payment.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(payment.amount).isEqualTo(50000L) },
            )
        }
    }

    @DisplayName("결제를 조회할 때,")
    @Nested
    inner class GetPayment {

        @DisplayName("ID로 조회하면 결제 정보를 반환한다.")
        @Test
        fun returnsPaymentById() {
            // arrange
            val saved = createPayment()

            // act
            val found = paymentService.getPayment(saved.id)

            // assert
            assertAll(
                { assertThat(found.id).isEqualTo(saved.id) },
                { assertThat(found.orderId).isEqualTo("ORDER-001") },
                { assertThat(found.status).isEqualTo(PaymentStatus.REQUESTED) },
            )
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외를 던진다.")
        @Test
        fun throwsNotFound_whenPaymentDoesNotExist() {
            // act
            val exception = assertThrows<CoreException> {
                paymentService.getPayment(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("orderId로 결제를 조회할 때,")
    @Nested
    inner class GetPaymentByOrderId {

        @DisplayName("해당 orderId의 결제 목록을 반환한다.")
        @Test
        fun returnsPaymentsByOrderId() {
            // arrange
            createPayment(orderId = "ORDER-001")
            createPayment(orderId = "ORDER-001", cardNo = "9999-8888-7777-6666")
            createPayment(orderId = "ORDER-002")

            // act
            val result = paymentService.getPaymentsByOrderId("ORDER-001")

            // assert
            assertThat(result).hasSize(2)
            assertThat(result).allMatch { it.orderId == "ORDER-001" }
        }

        @DisplayName("결제가 없으면 빈 리스트를 반환한다.")
        @Test
        fun returnsEmptyList_whenNoPayments() {
            // act
            val result = paymentService.getPaymentsByOrderId("ORDER-999")

            // assert
            assertThat(result).isEmpty()
        }
    }

    @DisplayName("결제 상태를 업데이트할 때,")
    @Nested
    inner class UpdateStatus {

        @DisplayName("PENDING으로 변경하면 transactionKey가 저장된다.")
        @Test
        fun updatesStatusToPendingWithTransactionKey() {
            // arrange
            val payment = createPayment()

            // act
            paymentService.markPending(payment.id, "txn-key-12345")

            // assert
            val updated = paymentService.getPayment(payment.id)
            assertAll(
                { assertThat(updated.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(updated.transactionKey).isEqualTo("txn-key-12345") },
            )
        }

        @DisplayName("SUCCESS로 변경하면 상태가 업데이트된다.")
        @Test
        fun updatesStatusToSuccess() {
            // arrange
            val payment = createPayment()
            paymentService.markPending(payment.id, "txn-key-12345")

            // act
            paymentService.markSuccess(payment.id)

            // assert
            val updated = paymentService.getPayment(payment.id)
            assertThat(updated.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("FAILED로 변경하면 실패 사유가 저장된다.")
        @Test
        fun updatesStatusToFailedWithReason() {
            // arrange
            val payment = createPayment()
            paymentService.markPending(payment.id, "txn-key-12345")

            // act
            paymentService.markFailed(payment.id, "한도 초과")

            // assert
            val updated = paymentService.getPayment(payment.id)
            assertAll(
                { assertThat(updated.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(updated.failReason).isEqualTo("한도 초과") },
            )
        }
    }
}
