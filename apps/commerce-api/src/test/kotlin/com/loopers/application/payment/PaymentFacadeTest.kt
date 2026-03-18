package com.loopers.application.payment

import com.loopers.application.order.OrderService
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import com.loopers.infrastructure.pg.PgApiResponse
import com.loopers.infrastructure.pg.PgCallbackRequest
import com.loopers.infrastructure.pg.PgPaymentResponse
import com.loopers.infrastructure.pg.PgTransactionDetailResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class PaymentFacadeTest {

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var pgPaymentClient: PgPaymentClient

    private lateinit var paymentFacade: PaymentFacade

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        val txManager = object : org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            override fun doGetTransaction(): Any = Any()
            override fun doBegin(transaction: Any, definition: org.springframework.transaction.TransactionDefinition) {}
            override fun doCommit(status: org.springframework.transaction.support.DefaultTransactionStatus) {}
            override fun doRollback(status: org.springframework.transaction.support.DefaultTransactionStatus) {}
        }
        paymentFacade = PaymentFacade(
            paymentService = paymentService,
            orderService = orderService,
            pgPaymentClient = pgPaymentClient,
            transactionManager = txManager,
            callbackUrl = "http://localhost:8080/api/v1/payments/callback",
        )
    }

    private fun createOrder(orderId: Long = 20260318000001L, userId: Long = 1L): Order {
        val order = Order(id = orderId, userId = userId)
        order.addItem(
            productId = 1L,
            productName = "에어맥스 90",
            brandName = "나이키",
            quantity = 1,
            unitPrice = BigDecimal("50000"),
        )
        return order
    }

    private fun createPayment(orderId: Long = 20260318000001L, userId: Long = 1L): Payment {
        val payment = Payment(
            orderId = orderId,
            userId = userId,
            amount = BigDecimal("50000"),
            cardType = "SAMSUNG",
            cardNo = "****-****-****-1451",
        )
        ReflectionTestUtils.setField(payment, "id", 1L)
        ReflectionTestUtils.setField(payment, "createdAt", ZonedDateTime.now())
        ReflectionTestUtils.setField(payment, "updatedAt", ZonedDateTime.now())
        return payment
    }

    private fun pgSuccessResponse() = PgApiResponse(
        meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
        data = PgPaymentResponse(transactionKey = "20260318:TR:abc123", status = "PENDING", reason = null),
    )

    @DisplayName("콜백을 수신할 때,")
    @Nested
    inner class HandleCallback {

        @DisplayName("SUCCESS 콜백이면, Payment → PAID, Order → PAID로 변경된다.")
        @Test
        fun marksPaid_whenSuccessCallback() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")
            val order = createOrder()

            whenever(paymentService.getPaymentByTransactionKey("20260318:TR:abc123")).thenReturn(payment)
            whenever(orderService.getOrder(20260318000001L)).thenReturn(order)

            val callbackRequest = PgCallbackRequest(
                transactionKey = "20260318:TR:abc123",
                orderId = "20260318000001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
                amount = 50000,
                status = "SUCCESS",
                reason = "정상 승인되었습니다.",
            )

            // act
            paymentFacade.handleCallback(callbackRequest)

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.PAID)
            assertThat(order.status).isEqualTo(OrderStatus.PAID)
        }

        @DisplayName("FAILED 콜백이면, Payment → FAILED, Order → FAILED로 변경된다.")
        @Test
        fun marksFailed_whenFailedCallback() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")
            val order = createOrder()

            whenever(paymentService.getPaymentByTransactionKey("20260318:TR:abc123")).thenReturn(payment)
            whenever(orderService.getOrder(20260318000001L)).thenReturn(order)

            val callbackRequest = PgCallbackRequest(
                transactionKey = "20260318:TR:abc123",
                orderId = "20260318000001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
                amount = 50000,
                status = "FAILED",
                reason = "한도초과입니다.",
            )

            // act
            paymentFacade.handleCallback(callbackRequest)

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.failReason).isEqualTo("한도초과입니다.")
            assertThat(order.status).isEqualTo(OrderStatus.FAILED)
        }

        @DisplayName("이미 처리된 결제(PAID)에 대한 콜백이면, 무시한다.")
        @Test
        fun ignoresCallback_whenAlreadyPaid() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")
            payment.markPaid()

            whenever(paymentService.getPaymentByTransactionKey("20260318:TR:abc123")).thenReturn(payment)

            val callbackRequest = PgCallbackRequest(
                transactionKey = "20260318:TR:abc123",
                orderId = "20260318000001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
                amount = 50000,
                status = "SUCCESS",
                reason = null,
            )

            // act
            paymentFacade.handleCallback(callbackRequest)

            // assert
            assertThat(payment.status).isEqualTo(PaymentStatus.PAID)
        }

        @DisplayName("존재하지 않는 transactionKey이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenTransactionKeyNotExists() {
            // arrange
            whenever(paymentService.getPaymentByTransactionKey("unknown")).thenReturn(null)

            val callbackRequest = PgCallbackRequest(
                transactionKey = "unknown",
                orderId = "20260318000001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
                amount = 50000,
                status = "SUCCESS",
                reason = null,
            )

            // act
            val exception = assertThrows<CoreException> {
                paymentFacade.handleCallback(callbackRequest)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("결제 상태를 조회할 때,")
    @Nested
    inner class GetPaymentStatus {

        @DisplayName("REQUESTED 상태면, PG에 직접 확인하여 동기화한다.")
        @Test
        fun syncFromPg_whenRequested() {
            // arrange
            val payment = createPayment()
            payment.markRequested("20260318:TR:abc123")
            val order = createOrder()

            whenever(orderService.getOrder(20260318000001L)).thenReturn(order)
            whenever(paymentService.getPaymentByOrderId(20260318000001L)).thenReturn(payment)
            whenever(pgPaymentClient.getPaymentStatus(eq("1"), eq("20260318:TR:abc123")))
                .thenReturn(
                    PgApiResponse(
                        meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
                        data = PgTransactionDetailResponse(
                            transactionKey = "20260318:TR:abc123",
                            orderId = "20260318000001",
                            cardType = "SAMSUNG",
                            cardNo = "1234-5678-9814-1451",
                            amount = 50000,
                            status = "SUCCESS",
                            reason = "정상 승인되었습니다.",
                        ),
                    ),
                )

            // act
            val result = paymentFacade.getPaymentStatus(1L, 20260318000001L)

            // assert
            assertThat(result.status).isEqualTo(PaymentStatus.PAID)
            assertThat(order.status).isEqualTo(OrderStatus.PAID)
        }

        @DisplayName("결제 정보가 없으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenPaymentNotExists() {
            // arrange
            val order = createOrder()
            whenever(orderService.getOrder(20260318000001L)).thenReturn(order)
            whenever(paymentService.getPaymentByOrderId(20260318000001L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                paymentFacade.getPaymentStatus(1L, 20260318000001L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("다른 사용자의 주문이면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsForbidden_whenNotOwner() {
            // arrange
            val order = createOrder(userId = 1L)
            whenever(orderService.getOrder(20260318000001L)).thenReturn(order)

            // act
            val exception = assertThrows<CoreException> {
                paymentFacade.getPaymentStatus(999L, 20260318000001L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
