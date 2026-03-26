package com.loopers.application.user.payment

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderSnapshot
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentStatusResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executors

@DisplayName("PaymentRecoveryService")
class PaymentRecoveryServiceTest {
    private val paymentKey = "11111111-1111-1111-1111-111111111111"

    private val paymentRepository: PaymentRepository = mock()
    private val orderRepository: OrderRepository = mock()
    private val pgPaymentPort: PgPaymentPort = mock()
    private val recoveryScheduler = Executors.newScheduledThreadPool(1)
    private val service = PaymentRecoveryService(
        paymentRepository,
        orderRepository,
        pgPaymentPort,
        recoveryScheduler,
    )

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun pendingPayment(
        id: Long = 200L,
        transactionKey: String? = "txn-abc-123",
    ): Payment = Payment.retrieve(
        id = id,
        orderId = 100L,
        userId = 1L,
        idempotencyKey = PaymentIdempotencyKey(paymentKey),
        status = Payment.Status.PENDING,
        cardType = "VISA",
        maskedCardNo = "************1234",
        amount = Money(BigDecimal("16000")),
        transactionKey = transactionKey,
        reasonCode = null,
        requestFingerprint = "fingerprint-001",
        createdAt = now,
    )

    private fun successPayment(id: Long = 200L): Payment = Payment.retrieve(
        id = id,
        orderId = 100L,
        userId = 1L,
        idempotencyKey = PaymentIdempotencyKey(paymentKey),
        status = Payment.Status.SUCCESS,
        cardType = "VISA",
        maskedCardNo = "************1234",
        amount = Money(BigDecimal("16000")),
        transactionKey = "txn-abc-123",
        reasonCode = null,
        requestFingerprint = "fingerprint-001",
        createdAt = now,
    )

    private fun pendingOrder(id: Long = 100L): Order = Order.retrieve(
        id = id,
        userId = 1L,
        idempotencyKey = IdempotencyKey("order-key-001"),
        status = Order.Status.PENDING,
        items = listOf(
            OrderItem.retrieve(
                id = 1L,
                snapshot = OrderSnapshot(
                    productId = 1L,
                    productName = "테스트 상품",
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    regularPrice = Money(BigDecimal("10000")),
                    sellingPrice = Money(BigDecimal("8000")),
                    thumbnailUrl = null,
                ),
                quantity = Quantity(2),
            ),
        ),
        createdAt = now,
    )

    @Nested
    @DisplayName("PENDING + transactionKey 있음 + PG 조회 SUCCESS -> 상태 반영")
    inner class WhenPgQuerySuccess {

        @Test
        @DisplayName("PG SUCCESS -> Payment=SUCCESS, Order=CREATED")
        fun queryAndReconcile_pgSuccess() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "SUCCESS", null))
            given(orderRepository.findById(100L)).willReturn(pendingOrder())
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)
            given(orderRepository.save(check<Order> { })).willAnswer { it.arguments[0] as Order }

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(paymentRepository).saveIfPending(
                check { payment ->
                    assertThat(payment.status).isEqualTo(Payment.Status.SUCCESS)
                    assertThat(payment.transactionKey).isEqualTo("txn-abc-123")
                },
            )
            verify(orderRepository).save(
                check { order ->
                    assertThat(order.status).isEqualTo(Order.Status.CREATED)
                },
            )
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey 있음 + PG 조회 FAILED -> 상태 반영")
    inner class WhenPgQueryFailed {

        @Test
        @DisplayName("PG FAILED -> Payment=FAILED")
        fun queryAndReconcile_pgFailed() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "FAILED", "한도초과"))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(paymentRepository).saveIfPending(
                check { payment ->
                    assertThat(payment.status).isEqualTo(Payment.Status.FAILED)
                    assertThat(payment.reasonCode).isEqualTo(PaymentReasonCode.LIMIT_EXCEEDED)
                },
            )
            verify(orderRepository, never()).save(check<Order> { })
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey 있음 + PG 아직 PENDING -> 변경 없음")
    inner class WhenPgStillPending {

        @Test
        @DisplayName("PG PENDING -> 상태 변경 없음")
        fun queryAndReconcile_pgStillPending() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "PENDING", null))

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
            verify(orderRepository, never()).save(check<Order> { })
        }
    }

    @Nested
    @DisplayName("이미 SUCCESS인 Payment -> early return, PG 조회 안 함")
    inner class WhenAlreadyTerminal {

        @Test
        @DisplayName("SUCCESS Payment -> PG 조회 없이 return")
        fun queryAndReconcile_alreadyTerminal() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(successPayment())

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(pgPaymentPort, never()).queryPaymentStatus("txn-abc-123", 1L)
            verify(paymentRepository, never()).save(check<Payment> { })
        }
    }

    @Nested
    @DisplayName("transactionKey null인 PENDING -> PG 조회 skip")
    inner class WhenTransactionKeyNull {

        @Test
        @DisplayName("transactionKey=null -> PG 조회 skip, callback이 유일한 복구 채널")
        fun queryAndReconcile_transactionKeyNull() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment(transactionKey = null))

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(pgPaymentPort, never()).queryPaymentStatus("txn-abc-123", 1L)
            verify(paymentRepository, never()).save(check<Payment> { })
        }
    }

    @Nested
    @DisplayName("PG 상태 조회 중 예외 발생 -> 예외를 삼키고 로그만 남김")
    inner class WhenPgQueryFails {

        @Test
        @DisplayName("PG 조회 예외 -> 상태 변경 없이 종료")
        fun queryAndReconcile_pgException() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willThrow(RuntimeException("PG 연결 실패"))

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
        }
    }

    @Nested
    @DisplayName("다른 경로가 먼저 terminal 반영하면 no-op")
    inner class WhenAlreadyReconciledByAnotherFlow {

        @Test
        @DisplayName("PG SUCCESS 조건부 update 영향 행 0 -> Order save 없이 종료")
        fun queryAndReconcile_pgSuccessAlreadyReconciled() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "SUCCESS", null))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(false)

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(orderRepository, never()).save(check<Order> { })
        }

        @Test
        @DisplayName("PG FAILED 조건부 update 영향 행 0 -> no-op")
        fun queryAndReconcile_pgFailedAlreadyReconciled() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "FAILED", "한도초과"))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(false)

            // act
            service.queryAndReconcile(200L)

            // assert
            verify(orderRepository, never()).save(check<Order> { })
        }
    }
}
