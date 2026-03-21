package com.loopers.application.user.payment

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderSnapshot
import com.loopers.domain.payment.DisplayStatus
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("PaymentDetailUseCase")
class PaymentDetailUseCaseTest {
    private val paymentKey = "11111111-1111-1111-1111-111111111111"

    private val paymentRepository: PaymentRepository = mock()
    private val orderRepository: OrderRepository = mock()
    private val paymentReadRepairService: PaymentReadRepairService = mock()
    private val useCase = PaymentDetailUseCase(paymentRepository, orderRepository, paymentReadRepairService)

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    private val userId = 1L
    private val paymentId = 200L
    private val orderId = 100L

    private fun pendingPayment(transactionKey: String? = "txn-abc-123"): Payment = Payment.retrieve(
        id = paymentId,
        orderId = orderId,
        userId = userId,
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

    private fun pendingOrder(): Order = Order.retrieve(
        id = orderId,
        userId = userId,
        idempotencyKey = IdempotencyKey("order-key-001"),
        status = Order.Status.PENDING,
        items = listOf(
            OrderItem.retrieve(
                id = 1L,
                snapshot = OrderSnapshot(
                    productId = 10L,
                    productName = "테스트 상품",
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    regularPrice = Money(BigDecimal("16000")),
                    sellingPrice = Money(BigDecimal("16000")),
                    thumbnailUrl = null,
                ),
                quantity = Quantity(1),
            ),
        ),
        createdAt = now,
    )

    private fun confirmedOrder(): Order = Order.retrieve(
        id = orderId,
        userId = userId,
        idempotencyKey = IdempotencyKey("order-key-001"),
        status = Order.Status.CREATED,
        items = listOf(
            OrderItem.retrieve(
                id = 1L,
                snapshot = OrderSnapshot(
                    productId = 10L,
                    productName = "테스트 상품",
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    regularPrice = Money(BigDecimal("16000")),
                    sellingPrice = Money(BigDecimal("16000")),
                    thumbnailUrl = null,
                ),
                quantity = Quantity(1),
            ),
        ),
        createdAt = now,
    )

    @Nested
    @DisplayName("이미 terminal 상태인 결제는 PG 조회 없이 바로 반환한다")
    inner class TerminalPayment {

        @Test
        @DisplayName("Payment=SUCCESS -> PG 조회 없이 반환, displayStatus=ORDER_CONFIRMED")
        fun detail_successPayment() {
            val payment = successPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(orderRepository.findById(orderId)).willReturn(confirmedOrder())

            val result = useCase.detail(paymentId, userId)

            assertAll(
                { assertThat(result.paymentId).isEqualTo(paymentId) },
                { assertThat(result.status).isEqualTo("SUCCESS") },
                { assertThat(result.displayStatus).isEqualTo(DisplayStatus.ORDER_CONFIRMED.name) },
                { assertThat(result.cardType).isEqualTo("VISA") },
                { assertThat(result.maskedCardNo).isEqualTo("************1234") },
            )
            verify(paymentReadRepairService, never()).repair(org.mockito.kotlin.any())
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey 있으면 read-repair를 수행한다")
    inner class ReadRepairWithTransactionKey {

        @Test
        @DisplayName("read-repair 결과 SUCCESS -> 성공 상태 반환")
        fun detail_readRepairSuccess() {
            val payment = pendingPayment(transactionKey = "txn-abc-123")
            val reconciledPayment = successPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(paymentReadRepairService.repair(payment)).willReturn(reconciledPayment)
            given(orderRepository.findById(orderId)).willReturn(confirmedOrder())

            val result = useCase.detail(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("SUCCESS") },
                { assertThat(result.displayStatus).isEqualTo(DisplayStatus.ORDER_CONFIRMED.name) },
            )
            verify(paymentReadRepairService).repair(payment)
        }

        @Test
        @DisplayName("read-repair 결과 FAILED -> 실패 상태 반환")
        fun detail_readRepairFailed() {
            val payment = pendingPayment(transactionKey = "txn-abc-123")
            val reconciledPayment = payment.fail(PaymentReasonCode.LIMIT_EXCEEDED)
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(paymentReadRepairService.repair(payment)).willReturn(reconciledPayment)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.detail(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("FAILED") },
                { assertThat(result.displayStatus).isEqualTo(DisplayStatus.REQUIRES_REPAYMENT.name) },
                { assertThat(result.reasonCode).isEqualTo(reconciledPayment.reasonCode?.name) },
            )
            verify(paymentReadRepairService).repair(payment)
        }

        @Test
        @DisplayName("read-repair 결과가 기존 PENDING이면 그대로 반환")
        fun detail_readRepairStillPending() {
            val payment = pendingPayment(transactionKey = "txn-abc-123")
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(paymentReadRepairService.repair(payment)).willReturn(payment)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.detail(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("PENDING") },
                { assertThat(result.displayStatus).isEqualTo(DisplayStatus.AWAITING_PAYMENT_RESULT.name) },
            )
            verify(paymentReadRepairService).repair(payment)
        }

        @Test
        @DisplayName("read-repair 서비스가 로컬 상태를 반환하면 그대로 응답한다")
        fun detail_readRepairPgQueryFails() {
            val payment = pendingPayment(transactionKey = "txn-abc-123")
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(paymentReadRepairService.repair(payment)).willReturn(payment)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.detail(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("PENDING") },
                { assertThat(result.displayStatus).isEqualTo(DisplayStatus.AWAITING_PAYMENT_RESULT.name) },
            )
            verify(paymentReadRepairService).repair(payment)
        }
    }

    @Nested
    @DisplayName("PENDING + transactionKey=null이면 PG 조회를 skip하고 로컬 상태를 반환한다")
    inner class PendingWithoutTransactionKey {

        @Test
        @DisplayName("transactionKey=null -> PG 조회 skip, callback 대기 상태")
        fun detail_noTransactionKeySkipsPgQuery() {
            val payment = pendingPayment(transactionKey = null)
            given(paymentRepository.findById(paymentId)).willReturn(payment)
            given(orderRepository.findById(orderId)).willReturn(pendingOrder())

            val result = useCase.detail(paymentId, userId)

            assertAll(
                { assertThat(result.status).isEqualTo("PENDING") },
                { assertThat(result.transactionKey).isNull() },
                { assertThat(result.displayStatus).isEqualTo(DisplayStatus.AWAITING_PAYMENT_RESULT.name) },
            )
            verify(paymentReadRepairService, never()).repair(org.mockito.kotlin.any())
        }
    }

    @Nested
    @DisplayName("결제가 존재하지 않으면 PAYMENT_NOT_FOUND")
    inner class PaymentNotFound {

        @Test
        @DisplayName("존재하지 않는 paymentId -> PAYMENT_NOT_FOUND")
        fun detail_paymentNotFound() {
            given(paymentRepository.findById(paymentId)).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.detail(paymentId, userId) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("다른 사용자의 결제를 조회하면 PAYMENT_NOT_FOUND")
    inner class WrongUser {

        @Test
        @DisplayName("userId 불일치 -> PAYMENT_NOT_FOUND")
        fun detail_wrongUser() {
            val payment = pendingPayment()
            given(paymentRepository.findById(paymentId)).willReturn(payment)

            val exception = assertThrows<CoreException> { useCase.detail(paymentId, 999L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_NOT_FOUND)
        }
    }
}
