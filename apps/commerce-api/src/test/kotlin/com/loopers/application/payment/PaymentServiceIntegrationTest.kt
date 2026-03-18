package com.loopers.application.payment

import com.loopers.domain.payment.PaymentStatus
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
import java.math.BigDecimal

@SpringBootTest
class PaymentServiceIntegrationTest @Autowired constructor(
    private val paymentService: PaymentService,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("결제를 생성할 때,")
    @Nested
    inner class CreatePayment {

        @DisplayName("정상적인 정보가 주어지면, DB에 INITIATED 상태로 저장된다.")
        @Test
        fun savesPayment_withInitiatedStatus() {
            // act
            val result = paymentService.createPayment(
                userId = 1L,
                orderId = 20260319000001L,
                amount = BigDecimal("50000"),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // assert
            val saved = paymentJpaRepository.findById(result.id).orElse(null)
            assertAll(
                { assertThat(saved).isNotNull() },
                { assertThat(saved!!.status).isEqualTo(PaymentStatus.INITIATED) },
                { assertThat(saved!!.cardNo).isEqualTo("****-****-****-1451") },
                { assertThat(saved!!.orderId).isEqualTo(20260319000001L) },
            )
        }
    }

    @DisplayName("결제를 조회할 때,")
    @Nested
    inner class GetPayment {

        @DisplayName("orderId로 조회하면, 해당 결제가 반환된다.")
        @Test
        fun returnsPayment_whenFoundByOrderId() {
            // arrange
            paymentService.createPayment(
                userId = 1L,
                orderId = 20260319000001L,
                amount = BigDecimal("50000"),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // act
            val result = paymentService.getPaymentByOrderId(20260319000001L)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.orderId).isEqualTo(20260319000001L)
        }

        @DisplayName("transactionKey로 조회하면, 해당 결제가 반환된다.")
        @Test
        fun returnsPayment_whenFoundByTransactionKey() {
            // arrange
            val payment = paymentService.createPayment(
                userId = 1L,
                orderId = 20260319000001L,
                amount = BigDecimal("50000"),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )
            payment.markRequested("20260319:TR:abc123")
            paymentJpaRepository.save(payment)

            // act
            val result = paymentService.getPaymentByTransactionKey("20260319:TR:abc123")

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result!!.transactionKey).isEqualTo("20260319:TR:abc123") },
                { assertThat(result!!.status).isEqualTo(PaymentStatus.REQUESTED) },
            )
        }

        @DisplayName("존재하지 않는 paymentId로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenPaymentNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                paymentService.getPayment(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
