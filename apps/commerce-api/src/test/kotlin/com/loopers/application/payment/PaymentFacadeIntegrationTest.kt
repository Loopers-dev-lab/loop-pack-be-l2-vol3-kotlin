package com.loopers.application.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class PaymentFacadeIntegrationTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("결제를 요청할 때,")
    @Nested
    inner class RequestPayment {

        @DisplayName("PG 호출 성공 시, 결제가 PENDING 상태로 DB에 저장된다.")
        @Test
        fun savesPaymentAsPending_whenPgCallSucceeds() {
            // arrange
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-key-123", status = "PENDING", reason = null),
            )

            // act
            val result = paymentFacade.requestPayment(
                userId = 1L,
                orderId = "ORDER-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // assert
            val saved = paymentService.getPayment(result.paymentId)
            assertAll(
                { assertThat(saved.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(saved.transactionKey).isEqualTo("txn-key-123") },
            )
        }

        @DisplayName("PG가 응답하지 못하면, 결제가 REQUESTED 상태로 DB에 저장된다 (Fallback).")
        @Test
        fun savesPaymentAsRequested_whenPgIsUnavailable() {
            // arrange
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)

            // act
            val result = paymentFacade.requestPayment(
                userId = 1L,
                orderId = "ORDER-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // assert
            val saved = paymentService.getPayment(result.paymentId)
            assertAll(
                { assertThat(saved.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(saved.transactionKey).isNull() },
                { assertThat(result.status).isEqualTo(PaymentStatus.REQUESTED) },
            )
        }
    }

    @DisplayName("결제 상태를 동기화할 때,")
    @Nested
    inner class SyncPaymentStatus {

        @DisplayName("PENDING 결제가 PG에서 SUCCESS로 확인되면, DB가 업데이트된다.")
        @Test
        fun updatesDbToSuccess_whenPgReturnsSuccess() {
            // arrange - PG 성공으로 PENDING 결제 생성
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-key-456", status = "PENDING", reason = null),
            )
            val created = paymentFacade.requestPayment(1L, "ORDER-002", CardType.KB, "9999-8888-7777-6666", 30000L)

            // PG 상태 조회 결과를 SUCCESS로 설정
            whenever(paymentGateway.getTransactionStatus(any(), any())).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-456",
                    orderId = "ORDER-002",
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // act
            val result = paymentFacade.syncPaymentStatus("ORDER-002")

            // assert
            assertAll(
                { assertThat(result).hasSize(1) },
                { assertThat(result.first().status).isEqualTo(PaymentStatus.SUCCESS) },
            )
            val saved = paymentService.getPayment(created.paymentId)
            assertThat(saved.status).isEqualTo(PaymentStatus.SUCCESS)
        }
    }

    @DisplayName("CircuitBreaker가 동작할 때,")
    @Nested
    inner class CircuitBreakerBehavior {

        @DisplayName("PG가 계속 응답하지 못해도 결제 요청은 REQUESTED 상태로 저장된다.")
        @Test
        fun alwaysSavesAsRequested_whenPgKeepsUnavailable() {
            // arrange
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)

            // act
            val results = (1..5).map { i ->
                paymentFacade.requestPayment(
                    userId = 1L,
                    orderId = "ORDER-${String.format("%03d", i)}",
                    cardType = CardType.SAMSUNG,
                    cardNo = "1234-5678-9012-3456",
                    amount = 50000L,
                )
            }

            // assert - 모든 결제가 REQUESTED 상태 (Fallback)
            assertThat(results).allMatch { it.status == PaymentStatus.REQUESTED }
        }
    }
}
