package com.loopers.domain.payment

import com.loopers.infrastructure.payment.PaymentJpaRepository
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
    private val paymentJpaRepository: PaymentJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("결제를 생성할 때, ")
    @Nested
    inner class CreatePayment {
        @DisplayName("유효한 결제 정보가 주어지면, 결제가 생성된다.")
        @Test
        fun createsPayment_whenValidDataIsProvided() {
            // arrange
            val payment = Payment(
                orderId = 1L,
                userId = 1L,
                cardType = CardType.SAMSUNG,
                cardNo = "xxxx-xxxx-xxxx-1234",
                amount = 50000L,
            )

            // act
            val result = paymentService.createPayment(payment)

            // assert
            val saved = paymentJpaRepository.findById(result.id).get()
            assertAll(
                { assertThat(saved.orderId).isEqualTo(1L) },
                { assertThat(saved.userId).isEqualTo(1L) },
                { assertThat(saved.cardType).isEqualTo(CardType.SAMSUNG) },
                { assertThat(saved.amount).isEqualTo(50000L) },
                { assertThat(saved.status).isEqualTo(PaymentStatus.PENDING) },
            )
        }
    }

    @DisplayName("트랜잭션 키로 결제를 조회할 때, ")
    @Nested
    inner class GetPaymentByTransactionKey {
        @DisplayName("트랜잭션 키가 일치하면, 결제 정보를 반환한다.")
        @Test
        fun returnsPayment_whenTransactionKeyMatches() {
            // arrange
            val payment = paymentJpaRepository.save(
                Payment(orderId = 1L, userId = 1L, cardType = CardType.KB, cardNo = "xxxx-xxxx-xxxx-5678", amount = 30000L),
            )
            payment.assignTransactionKey("20250816:TR:abc123")
            paymentJpaRepository.save(payment)

            // act
            val result = paymentService.getPaymentByTransactionKey("20250816:TR:abc123")

            // assert
            assertThat(result.id).isEqualTo(payment.id)
        }

        @DisplayName("트랜잭션 키가 없으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenTransactionKeyNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                paymentService.getPaymentByTransactionKey("nonexistent")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("주문에 성공한 결제가 있는지 확인할 때, ")
    @Nested
    inner class HasSuccessfulPayment {
        @DisplayName("SUCCESS 상태 결제가 있으면, true를 반환한다.")
        @Test
        fun returnsTrue_whenSuccessfulPaymentExists() {
            // arrange
            val payment = paymentJpaRepository.save(
                Payment(orderId = 1L, userId = 1L, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-1234", amount = 50000L),
            )
            payment.complete(PaymentStatus.SUCCESS, "정상 승인")
            paymentJpaRepository.save(payment)

            // act
            val result = paymentService.hasSuccessfulPayment(1L)

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("SUCCESS 상태 결제가 없으면, false를 반환한다.")
        @Test
        fun returnsFalse_whenNoSuccessfulPayment() {
            // arrange
            paymentJpaRepository.save(
                Payment(orderId = 1L, userId = 1L, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-1234", amount = 50000L),
            )

            // act
            val result = paymentService.hasSuccessfulPayment(1L)

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("주문별 결제 목록을 조회할 때, ")
    @Nested
    inner class GetPaymentsByOrderId {
        @DisplayName("해당 주문의 결제 목록을 반환한다.")
        @Test
        fun returnsPayments_whenPaymentsExistForOrder() {
            // arrange
            paymentJpaRepository.save(
                Payment(orderId = 1L, userId = 1L, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-1234", amount = 50000L),
            )
            paymentJpaRepository.save(
                Payment(orderId = 1L, userId = 1L, cardType = CardType.KB, cardNo = "xxxx-xxxx-xxxx-5678", amount = 50000L),
            )
            paymentJpaRepository.save(
                Payment(orderId = 2L, userId = 1L, cardType = CardType.HYUNDAI, cardNo = "xxxx-xxxx-xxxx-9012", amount = 30000L),
            )

            // act
            val result = paymentService.getPaymentsByOrderId(1L)

            // assert
            assertThat(result).hasSize(2)
        }
    }
}
