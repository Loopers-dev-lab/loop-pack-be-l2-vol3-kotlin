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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.BDDMockito.given
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("PaymentCallbackUseCase")
class PaymentCallbackUseCaseTest {

    private val paymentRepository: PaymentRepository = mock()
    private val orderRepository: OrderRepository = mock()
    private val useCase = PaymentCallbackUseCase(paymentRepository, orderRepository)

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun pendingPayment(
        id: Long = 200L,
        orderId: Long = 100L,
        transactionKey: String? = "txn-abc-123",
    ): Payment = Payment.retrieve(
        id = id,
        orderId = orderId,
        userId = 1L,
        idempotencyKey = PaymentIdempotencyKey("pay-key-001"),
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
        idempotencyKey = PaymentIdempotencyKey("pay-key-001"),
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

    private fun successCommand(paymentId: Long = 200L): PaymentCallbackCommand = PaymentCallbackCommand(
        paymentId = paymentId,
        transactionKey = "txn-abc-123",
        status = "SUCCESS",
        reason = null,
    )

    private fun failedCommand(paymentId: Long = 200L): PaymentCallbackCommand = PaymentCallbackCommand(
        paymentId = paymentId,
        transactionKey = "txn-abc-123",
        status = "FAILED",
        reason = "한도초과",
    )

    @Nested
    @DisplayName("PG SUCCESS callback을 수신하면 Payment=SUCCESS, Order=CREATED로 전이한다")
    inner class WhenSuccessCallback {

        @Test
        @DisplayName("PENDING Payment에 SUCCESS callback -> Payment.succeed + Order.confirm")
        fun handleCallback_success() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(orderRepository.findById(100L)).willReturn(pendingOrder())
            given(paymentRepository.save(check<Payment> { })).willAnswer { it.arguments[0] as Payment }
            given(orderRepository.save(check<Order> { })).willAnswer { it.arguments[0] as Order }

            // act
            useCase.handleCallback(successCommand())

            // assert
            verify(paymentRepository).save(
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
    @DisplayName("PG FAILED callback을 수신하면 Payment=FAILED로 전이한다")
    inner class WhenFailedCallback {

        @Test
        @DisplayName("PENDING Payment에 FAILED callback -> Payment.fail, Order는 PENDING 유지")
        fun handleCallback_failed() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(pendingPayment())
            given(paymentRepository.save(check<Payment> { })).willAnswer { it.arguments[0] as Payment }

            // act
            useCase.handleCallback(failedCommand())

            // assert
            verify(paymentRepository).save(
                check { payment ->
                    assertThat(payment.status).isEqualTo(Payment.Status.FAILED)
                    assertThat(payment.reasonCode).isEqualTo(PaymentReasonCode.LIMIT_EXCEEDED)
                },
            )
            verify(orderRepository, never()).save(check<Order> { })
        }
    }

    @Nested
    @DisplayName("이미 terminal 상태인 Payment에 중복 callback이 오면 no-op 처리한다")
    inner class WhenDuplicateCallback {

        @Test
        @DisplayName("SUCCESS Payment에 중복 callback -> no-op")
        fun handleCallback_duplicateNoOp() {
            // arrange
            given(paymentRepository.findById(200L)).willReturn(successPayment())

            // act
            useCase.handleCallback(successCommand())

            // assert
            verify(paymentRepository, never()).save(check<Payment> { })
            verify(orderRepository, never()).save(check<Order> { })
        }
    }

    @Nested
    @DisplayName("존재하지 않는 paymentId로 callback이 오면 무시한다")
    inner class WhenPaymentNotFound {

        @Test
        @DisplayName("paymentId 조회 null -> 무시")
        fun handleCallback_paymentNotFound() {
            // arrange
            given(paymentRepository.findById(999L)).willReturn(null)

            // act
            useCase.handleCallback(successCommand(paymentId = 999L))

            // assert
            verify(paymentRepository, never()).save(check<Payment> { })
        }
    }
}
