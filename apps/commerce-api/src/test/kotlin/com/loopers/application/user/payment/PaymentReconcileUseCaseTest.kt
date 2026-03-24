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
import com.loopers.domain.payment.PgPaymentOrderResponse
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("PaymentReconcileUseCase")
class PaymentReconcileUseCaseTest {
    private val paymentKey = "11111111-1111-1111-1111-111111111111"

    private val paymentRepository: PaymentRepository = mock()
    private val orderRepository: OrderRepository = mock()
    private val pgPaymentPort: PgPaymentPort = mock()
    private val useCase = PaymentReconcileUseCase(paymentRepository, orderRepository, pgPaymentPort)

    private val now = ZonedDateTime.of(2026, 3, 24, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    private val paymentId = 200L
    private val orderId = 100L
    private val userId = 1L

    private fun timeoutPendingPayment(
        transactionKey: String? = null,
        reasonCode: PaymentReasonCode? = PaymentReasonCode.TIMEOUT_UNCERTAIN,
    ): Payment = Payment.retrieve(
        id = paymentId,
        orderId = orderId,
        userId = userId,
        idempotencyKey = PaymentIdempotencyKey(paymentKey),
        status = Payment.Status.PENDING,
        cardType = "VISA",
        maskedCardNo = "************1234",
        amount = Money(BigDecimal("16000")),
        transactionKey = transactionKey,
        reasonCode = reasonCode,
        requestFingerprint = "fingerprint-001",
        createdAt = now,
    )

    private fun successPayment(): Payment = Payment.retrieve(
        id = paymentId,
        orderId = orderId,
        userId = userId,
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

    private fun pendingOrder(status: Order.Status = Order.Status.PENDING): Order = Order.retrieve(
        id = orderId,
        userId = userId,
        idempotencyKey = IdempotencyKey("order-key-001"),
        status = status,
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

    private fun pgOrderResponse(vararg transactions: PgPaymentOrderResponse.Transaction): PgPaymentOrderResponse =
        PgPaymentOrderResponse(
            orderId = orderId.toString(),
            transactions = transactions.toList(),
        )

    private fun transaction(
        transactionKey: String = "txn-abc-123",
        cardType: String = "VISA",
        cardNo: String = "4111111111111234",
        amount: Long = 16000L,
        status: String = "SUCCESS",
        reason: String? = null,
    ): PgPaymentOrderResponse.Transaction = PgPaymentOrderResponse.Transaction(
        transactionKey = transactionKey,
        orderId = orderId.toString(),
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
        status = status,
        reason = reason,
    )

    @Test
    @DisplayName("본인 결제가 아니면 PAYMENT_NOT_FOUND")
    fun reconcile_notOwnedPayment() {
        val payment = timeoutPendingPayment()
        given(paymentRepository.findById(paymentId)).willReturn(payment)

        val exception = assertThrows<CoreException> {
            useCase.reconcile(paymentId, userId + 1)
        }

        assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_NOT_FOUND)
    }

    @Nested
    @DisplayName("대상이 아니면 상태 변경 없이 NOT_APPLICABLE")
    inner class NotApplicable {

        @Test
        @DisplayName("transactionKey가 이미 있으면 PG 조회를 생략한다")
        fun reconcile_notApplicable() {
            val payment = timeoutPendingPayment(transactionKey = "txn-known")
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.reconcile(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("PENDING") },
                { assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.NOT_APPLICABLE.name) },
            )
            verify(pgPaymentPort, never()).queryPaymentsByOrderId(orderId, userId)
        }
    }

    @Nested
    @DisplayName("PG order 조회 기반 수동 복구")
    inner class ManualReconcile {

        @Test
        @DisplayName("후보가 없으면 로컬 상태 유지 + NOT_FOUND_IN_PG")
        fun reconcile_noCandidate() {
            val payment = timeoutPendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(pgOrderResponse())
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.reconcile(paymentId, userId)

            assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.NOT_FOUND_IN_PG.name)
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
        }

        @Test
        @DisplayName("후보가 여러 건이면 로컬 상태 유지 + AMBIGUOUS")
        fun reconcile_ambiguous() {
            val payment = timeoutPendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(
                    transaction(transactionKey = "txn-1"),
                    transaction(transactionKey = "txn-2"),
                ),
            )
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.reconcile(paymentId, userId)

            assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.AMBIGUOUS.name)
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
        }

        @Test
        @DisplayName("후보 1건이 PENDING이면 transactionKey를 저장하고 STILL_PENDING")
        fun reconcile_stillPending() {
            val payment = timeoutPendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(transaction(status = "PENDING", reason = null)),
            )
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.reconcile(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("PENDING") },
                { assertThat(result.transactionKey).isEqualTo("txn-abc-123") },
                { assertThat(result.reasonCode).isNull() },
                { assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.STILL_PENDING.name) },
            )
            verify(paymentRepository).saveIfPending(
                check {
                    assertThat(it.status).isEqualTo(Payment.Status.PENDING)
                    assertThat(it.transactionKey).isEqualTo("txn-abc-123")
                    assertThat(it.reasonCode).isNull()
                },
            )
            verify(orderRepository, never()).save(check<Order> { })
        }

        @Test
        @DisplayName("후보 1건이 SUCCESS면 Payment=SUCCESS, Order=CREATED")
        fun reconcile_success() {
            val payment = timeoutPendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(transaction(status = "SUCCESS")),
            )
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder(), pendingOrder(Order.Status.CREATED))
            given(orderRepository.save(check<Order> { })).willAnswer { it.arguments[0] as Order }

            val result = useCase.reconcile(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("SUCCESS") },
                { assertThat(result.displayStatus).isEqualTo("ORDER_CONFIRMED") },
                { assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.RESOLVED_SUCCESS.name) },
            )
            verify(orderRepository).save(check { assertThat(it.status).isEqualTo(Order.Status.CREATED) })
        }

        @Test
        @DisplayName("후보 1건이 FAILED면 Payment=FAILED")
        fun reconcile_failed() {
            val payment = timeoutPendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(transaction(status = "FAILED", reason = "한도초과")),
            )
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(true)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.reconcile(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("FAILED") },
                { assertThat(result.reasonCode).isEqualTo(PaymentReasonCode.LIMIT_EXCEEDED.name) },
                { assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.RESOLVED_FAILURE.name) },
            )
        }

        @Test
        @DisplayName("다른 경로가 먼저 반영했으면 최신 상태를 반환한다")
        fun reconcile_alreadyReconciled() {
            val payment = timeoutPendingPayment()
            val latestPayment = successPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment, latestPayment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willReturn(
                pgOrderResponse(transaction(status = "SUCCESS")),
            )
            given(paymentRepository.saveIfPending(check<Payment> { })).willReturn(false)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder(Order.Status.CREATED))

            val result = useCase.reconcile(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("SUCCESS") },
                { assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.ALREADY_RECONCILED.name) },
            )
        }

        @Test
        @DisplayName("PG order 조회가 실패하면 로컬 상태 유지 + QUERY_FAILED")
        fun reconcile_queryFailed() {
            val payment = timeoutPendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(pgPaymentPort.queryPaymentsByOrderId(orderId, userId)).willThrow(RuntimeException("timeout"))
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.reconcile(paymentId, userId)

            assertThat(result.reconcileStatus).isEqualTo(PaymentResult.ReconcileStatus.QUERY_FAILED.name)
            verify(paymentRepository, never()).saveIfPending(check<Payment> { })
        }
    }
}
