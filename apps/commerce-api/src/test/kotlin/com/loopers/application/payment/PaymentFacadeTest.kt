package com.loopers.application.payment

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductService
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

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var couponService: CouponService

    private lateinit var paymentFacade: PaymentFacade

    @BeforeEach
    fun setUp() {
        paymentFacade = PaymentFacade(
            paymentService, paymentGateway, orderService, productService, couponService,
            "http://localhost:8080/api/v1/payments/callback",
        )
    }

    private fun createPayment(id: Long = 1L, orderId: String = "100"): Payment {
        val payment = Payment(
            userId = 1L,
            orderId = orderId,
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

        @DisplayName("PG 호출 실패 + PG에도 결제가 없으면, FAILED 상태로 변경한다.")
        @Test
        fun marksFailed_whenPgRequestFailedAndNoTransactionInPg() {
            // arrange
            val payment = createPayment()
            whenever(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(payment)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)
            whenever(paymentGateway.getTransactionsByOrderId(any(), any()))
                .thenReturn(emptyList())
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
            verify(paymentService).markFailed(eq(payment.id), any())
        }

        @DisplayName("PG 호출이 실패해도 PG에 결제가 생성되었으면, PENDING으로 전환한다.")
        @Test
        fun marksPending_whenPgRequestFailedButTransactionExistsInPg() {
            // arrange
            val payment = createPayment()
            whenever(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(payment)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)
            whenever(paymentGateway.getTransactionsByOrderId(any(), eq("ORDER-001")))
                .thenReturn(listOf(PaymentGatewayResponse(transactionKey = "txn-key-123", status = "PENDING", reason = null)))
            whenever(paymentService.getPaymentsByOrderId("ORDER-001")).thenReturn(listOf(payment))
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

        @DisplayName("PG가 즉시 FAILED를 반환하면, 복구 조회 없이 즉시 FAILED 처리한다.")
        @Test
        fun marksFailed_whenPgReturnsImmediateFailure() {
            // arrange
            val payment = createPayment()
            whenever(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(payment)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(PaymentGatewayResponse(transactionKey = "txn-fail-001", status = "FAILED", reason = "카드 한도 초과"))
            whenever(paymentService.getPayment(1L)).thenReturn(payment)

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
                { verify(paymentService).markFailed(payment.id, "카드 한도 초과") },
                { verify(paymentService, never()).markPending(any(), any()) },
                { verify(paymentGateway, never()).getTransactionsByOrderId(any(), any()) },
            )
        }

        @DisplayName("PG 복구 조회 시, 같은 orderId의 과거 거래는 제외하고 현재 요청에 해당하는 거래만 선택한다.")
        @Test
        fun recoversOnlyCurrentTransaction_excludingPastTransactions() {
            // arrange
            val currentPayment = createPayment(id = 3L, orderId = "ORDER-001")
            val pastPayment1 = createPayment(id = 1L, orderId = "ORDER-001").apply {
                markPending("txn-old-001")
            }
            val pastPayment2 = createPayment(id = 2L, orderId = "ORDER-001").apply {
                markPending("txn-old-002")
            }

            whenever(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(currentPayment)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)
            whenever(paymentGateway.getTransactionsByOrderId(any(), eq("ORDER-001")))
                .thenReturn(
                    listOf(
                        PaymentGatewayResponse(transactionKey = "txn-old-001", status = "FAILED", reason = null),
                        PaymentGatewayResponse(transactionKey = "txn-old-002", status = "FAILED", reason = null),
                        PaymentGatewayResponse(transactionKey = "txn-new-003", status = "PENDING", reason = null),
                    ),
                )
            whenever(paymentService.getPaymentsByOrderId("ORDER-001"))
                .thenReturn(listOf(pastPayment1, pastPayment2, currentPayment))
            whenever(paymentService.getPayment(3L)).thenReturn(currentPayment)

            // act
            paymentFacade.requestPayment(
                userId = 1L,
                orderId = "ORDER-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // assert — 과거 거래(txn-old-001, txn-old-002)를 건너뛰고 txn-new-003을 선택
            verify(paymentService).markPending(currentPayment.id, "txn-new-003")
        }
    }

    @DisplayName("콜백을 처리할 때,")
    @Nested
    inner class HandleCallback {

        @DisplayName("콜백 상태를 그대로 신뢰하지 않고, PG 조회로 실제 상태를 확인한다.")
        @Test
        fun verifiesCallbackStatusWithPg() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentByTransactionKey("txn-key-123")).thenReturn(payment)

            // 콜백은 SUCCESS라고 하지만, PG 실제 상태는 FAILED
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = "100",
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )
            // 보상 트랜잭션을 위한 주문 조회
            val order = com.loopers.domain.order.Order(userId = 1L)
            ReflectionTestUtils.setField(order, "id", 100L)
            whenever(orderService.getOrderById(100L)).thenReturn(order)

            // act — 콜백은 SUCCESS로 전달
            paymentFacade.handleCallback("txn-key-123", "SUCCESS", null)

            // assert — PG 조회 결과(FAILED)를 따름
            verify(paymentService).markFailed(payment.id, "한도 초과")
            verify(paymentService, never()).markSuccess(any())
        }

        @DisplayName("SUCCESS 콜백이면, 주문 상태를 CONFIRMED로 변경한다.")
        @Test
        fun confirmsOrder_whenPaymentSucceeds() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentByTransactionKey("txn-key-123")).thenReturn(payment)
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = "100",
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // act
            paymentFacade.handleCallback("txn-key-123", "SUCCESS", null)

            // assert
            verify(orderService).changeStatus(100L, OrderStatus.CONFIRMED)
        }

        @DisplayName("PG 재조회 실패 시, 검증되지 않은 콜백으로 상태를 변경하지 않고 PENDING을 유지한다.")
        @Test
        fun keepsPending_whenPgVerificationFails() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentByTransactionKey("txn-key-123")).thenReturn(payment)

            // PG 재조회 실패 (null 반환)
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123")))
                .thenReturn(null)

            // act — 콜백은 SUCCESS로 전달되지만 PG 검증 불가
            paymentFacade.handleCallback("txn-key-123", "SUCCESS", null)

            // assert — 결제/주문 상태를 변경하지 않음
            assertAll(
                { verify(paymentService, never()).markSuccess(any()) },
                { verify(paymentService, never()).markFailed(any(), any()) },
                { verify(orderService, never()).changeStatus(any(), any()) },
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

        @DisplayName("PG가 응답하지 못하면, 기존 상태를 유지한다.")
        @Test
        fun keepsCurrentStatus_whenPgIsUnavailable() {
            // arrange
            val payment = createPayment()
            payment.markPending("txn-key-123")
            whenever(paymentService.getPaymentsByOrderId("ORDER-001")).thenReturn(listOf(payment))
            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-123")))
                .thenReturn(null)

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
