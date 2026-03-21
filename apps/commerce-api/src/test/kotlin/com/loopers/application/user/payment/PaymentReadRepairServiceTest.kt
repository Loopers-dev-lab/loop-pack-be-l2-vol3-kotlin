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
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("PaymentReadRepairService")
class PaymentReadRepairServiceTest {
    private val paymentKey = "11111111-1111-1111-1111-111111111111"

    private val paymentRepository: PaymentRepository = mock()
    private val orderRepository: OrderRepository = mock()
    private val pgPaymentPort: PgPaymentPort = mock()
    private val service = PaymentReadRepairService(paymentRepository, orderRepository, pgPaymentPort)

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

    private fun failedPayment(id: Long = 200L): Payment = Payment.retrieve(
        id = id,
        orderId = 100L,
        userId = 1L,
        idempotencyKey = PaymentIdempotencyKey(paymentKey),
        status = Payment.Status.FAILED,
        cardType = "VISA",
        maskedCardNo = "************1234",
        amount = Money(BigDecimal("16000")),
        transactionKey = "txn-abc-123",
        reasonCode = PaymentReasonCode.LIMIT_EXCEEDED,
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
        fun repair_pgSuccess() {
            val payment = pendingPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "SUCCESS", null))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)
            given(orderRepository.findById(100L)).willReturn(pendingOrder())
            given(orderRepository.save(check<Order> { })).willAnswer { it.arguments[0] as Order }

            val result = service.repair(payment)

            assertThat(result.status).isEqualTo(Payment.Status.SUCCESS)
            verify(paymentRepository).saveIfPending(
                check {
                    assertThat(it.status).isEqualTo(Payment.Status.SUCCESS)
                    assertThat(it.transactionKey).isEqualTo("txn-abc-123")
                },
            )
            verify(orderRepository).save(
                check { assertThat(it.status).isEqualTo(Order.Status.CREATED) },
            )
        }

        @Test
        @DisplayName("주문 저장 실패는 삼키지 않고 예외를 올린다")
        fun repair_orderSaveFails() {
            val payment = pendingPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "SUCCESS", null))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)
            given(orderRepository.findById(100L)).willReturn(pendingOrder())
            given(orderRepository.save(check<Order> { })).willThrow(RuntimeException("order save failed"))

            val exception = assertThrows<RuntimeException> { service.repair(payment) }

            assertThat(exception.message).isEqualTo("order save failed")
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey 있음 + PG 조회 FAILED -> 상태 반영")
    inner class WhenPgQueryFailed {

        @Test
        @DisplayName("PG FAILED -> Payment=FAILED")
        fun repair_pgFailed() {
            val payment = pendingPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "FAILED", "한도초과"))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)

            val result = service.repair(payment)

            assertThat(result.status).isEqualTo(Payment.Status.FAILED)
            assertThat(result.reasonCode).isEqualTo(PaymentReasonCode.LIMIT_EXCEEDED)
            verify(orderRepository, never()).save(check<Order> { })
        }

        @Test
        @DisplayName("다른 경로가 먼저 FAILED 반영했으면 최신 상태를 다시 읽어 반환")
        fun repair_pgFailedAlreadyReconciled() {
            val payment = pendingPayment()
            val latestPayment = failedPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "FAILED", "한도초과"))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(false)
            given(paymentRepository.findById(200L)).willReturn(latestPayment)

            val result = service.repair(payment)

            assertThat(result).isEqualTo(latestPayment)
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey 있음 + PG 조회 PENDING -> 변경 없음")
    inner class WhenPgStillPending {

        @Test
        @DisplayName("PG PENDING -> 기존 Payment 반환")
        fun repair_pgStillPending() {
            val payment = pendingPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "PENDING", null))

            val result = service.repair(payment)

            assertThat(result).isEqualTo(payment)
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
        }
    }

    @Nested
    @DisplayName("PG 상태 조회 실패 -> 로컬 상태 유지")
    inner class WhenPgQueryFails {

        @Test
        @DisplayName("PG 조회 예외 -> 기존 Payment 반환")
        fun repair_pgException() {
            val payment = pendingPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willThrow(RuntimeException("PG 연결 실패"))

            val result = service.repair(payment)

            assertThat(result).isEqualTo(payment)
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
        }
    }

    @Nested
    @DisplayName("다른 경로가 먼저 terminal 반영하면 최신 상태를 반환")
    inner class WhenAlreadyReconciledByAnotherFlow {

        @Test
        @DisplayName("PG SUCCESS 조건부 update 영향 행 0 -> 최신 SUCCESS 반환")
        fun repair_pgSuccessAlreadyReconciled() {
            val payment = pendingPayment()
            val latestPayment = successPayment()
            given(pgPaymentPort.queryPaymentStatus("txn-abc-123", 1L))
                .willReturn(PgPaymentStatusResponse("txn-abc-123", "SUCCESS", null))
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(false)
            given(paymentRepository.findById(200L)).willReturn(latestPayment)

            val result = service.repair(payment)

            assertThat(result).isEqualTo(latestPayment)
            verify(orderRepository, never()).save(check<Order> { })
        }
    }
}
