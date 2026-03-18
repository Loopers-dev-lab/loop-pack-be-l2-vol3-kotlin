package com.loopers.application.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class PaymentFacadeTest {

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var paymentGateway: PaymentGateway

    private lateinit var paymentFacade: PaymentFacade

    @BeforeEach
    fun setUp() {
        paymentFacade = PaymentFacade(paymentService, paymentGateway, "http://localhost:8080/api/v1/payments/callback")
    }

    private fun createPayment(id: Long = 1L): Payment {
        val payment = Payment(
            userId = 1L,
            orderId = "ORDER-001",
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 50000L,
        )
        ReflectionTestUtils.setField(payment, "id", id)
        return payment
    }

    @DisplayName("결제를 요청할 때,")
    @Nested
    inner class RequestPayment {

        @DisplayName("PG 호출이 성공하면, PENDING 상태로 변경한다.")
        @Test
        fun marksAsPending_whenPgCallSucceeds() {
            // arrange
            val payment = createPayment()
            whenever(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(payment)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-key-123", status = "PENDING", reason = null),
            )
            whenever(paymentService.getPayment(1L)).thenReturn(payment)

            // act
            paymentFacade.requestPayment(
                userId = 1L,
                orderId = "ORDER-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // assert
            verify(paymentService).markPending(payment.id, "txn-key-123")
        }

        @DisplayName("PG 호출이 실패하면, REQUESTED 상태를 유지한다 (Fallback).")
        @Test
        fun keepsRequestedStatus_whenPgCallFails() {
            // arrange
            val payment = createPayment()
            whenever(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(payment)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenThrow(RuntimeException("PG 연결 실패"))

            // act
            val result = paymentFacade.requestPayment(
                userId = 1L,
                orderId = "ORDER-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.REQUESTED) },
                { verify(paymentService, never()).markPending(any(), any()) },
            )
        }
    }

    @DisplayName("결제 상태를 동기화할 때,")
    @Nested
    inner class SyncPaymentStatus {

        @DisplayName("PG 조회 결과가 SUCCESS이면, 결제 상태를 SUCCESS로 업데이트한다.")
        @Test
        fun updatesStatusToSuccess_whenPgReturnsSuccess() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentsByOrderId("ORDER-001")).thenReturn(listOf(payment))
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = "ORDER-001",
                    status = "SUCCESS",
                    reason = null,
                ),
            )
            whenever(paymentService.getPayment(1L)).thenReturn(payment)

            // act
            paymentFacade.syncPaymentStatus("ORDER-001")

            // assert
            verify(paymentService).markSuccess(payment.id)
        }

        @DisplayName("PG 조회 결과가 FAILED이면, 결제 상태를 FAILED로 업데이트한다.")
        @Test
        fun updatesStatusToFailed_whenPgReturnsFailed() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentsByOrderId("ORDER-001")).thenReturn(listOf(payment))
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = "ORDER-001",
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )
            whenever(paymentService.getPayment(1L)).thenReturn(payment)

            // act
            paymentFacade.syncPaymentStatus("ORDER-001")

            // assert
            verify(paymentService).markFailed(payment.id, "한도 초과")
        }

        @DisplayName("PG 조회 결과가 PENDING이면, 상태를 변경하지 않는다.")
        @Test
        fun doesNotChangeStatus_whenPgReturnsPending() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentsByOrderId("ORDER-001")).thenReturn(listOf(payment))
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = "ORDER-001",
                    status = "PENDING",
                    reason = null,
                ),
            )

            // act
            paymentFacade.syncPaymentStatus("ORDER-001")

            // assert
            verify(paymentService, never()).markSuccess(any())
            verify(paymentService, never()).markFailed(any(), any())
        }

        @DisplayName("PG 조회에 실패하면, 기존 상태를 유지한다.")
        @Test
        fun keepsCurrentStatus_whenPgQueryFails() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentsByOrderId("ORDER-001")).thenReturn(listOf(payment))
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123")))
                .thenThrow(RuntimeException("PG 연결 실패"))

            // act
            val result = paymentFacade.syncPaymentStatus("ORDER-001")

            // assert
            assertAll(
                { assertThat(result).hasSize(1) },
                { assertThat(result.first().status).isEqualTo(PaymentStatus.PENDING) },
                { verify(paymentService, never()).markSuccess(any()) },
            )
        }
    }
}
