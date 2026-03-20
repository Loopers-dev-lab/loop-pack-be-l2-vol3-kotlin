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
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("PaymentCreateUseCase")
class PaymentCreateUseCaseTest {

    private val orderRepository: OrderRepository = mock()
    private val paymentRepository: PaymentRepository = mock()
    private val useCase = PaymentCreateUseCase(orderRepository, paymentRepository)

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun command(
        userId: Long = 1L,
        orderId: Long = 100L,
        idempotencyKey: String = "pay-key-001",
        cardType: String = "VISA",
        cardNo: String = "4111111111111234",
    ): PaymentCreateCommand = PaymentCreateCommand(
        userId = userId,
        orderId = orderId,
        idempotencyKey = idempotencyKey,
        cardType = cardType,
        cardNo = cardNo,
    )

    private fun pendingOrder(
        id: Long = 100L,
        userId: Long = 1L,
    ): Order = Order.retrieve(
        id = id,
        userId = userId,
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

    private fun createdOrder(id: Long = 100L): Order = Order.retrieve(
        id = id,
        userId = 1L,
        idempotencyKey = IdempotencyKey("order-key-001"),
        status = Order.Status.CREATED,
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

    private fun existingPayment(
        id: Long = 200L,
        orderId: Long = 100L,
        fingerprint: String = PaymentCreateUseCase.computeFingerprint(100L, "VISA", "4111111111111234"),
    ): Payment = Payment.retrieve(
        id = id,
        orderId = orderId,
        userId = 1L,
        idempotencyKey = PaymentIdempotencyKey("pay-key-001"),
        status = Payment.Status.PENDING,
        cardType = "VISA",
        maskedCardNo = "************1234",
        amount = Money(BigDecimal("16000")),
        transactionKey = null,
        reasonCode = null,
        requestFingerprint = fingerprint,
        createdAt = now,
    )

    private fun savedPayment(payment: Payment): Payment = Payment.retrieve(
        id = 200L,
        orderId = payment.orderId,
        userId = payment.userId,
        idempotencyKey = payment.idempotencyKey,
        status = payment.status,
        cardType = payment.cardType,
        maskedCardNo = payment.maskedCardNo,
        amount = payment.amount,
        transactionKey = payment.transactionKey,
        reasonCode = payment.reasonCode,
        requestFingerprint = payment.requestFingerprint,
        createdAt = now,
    )

    @Nested
    @DisplayName("정상 결제 요청 시 NewlyCreated를 반환한다")
    inner class WhenNormalRequest {

        @Test
        @DisplayName("Payment 생성 → NewlyCreated(paymentId, PENDING, displayStatus=AWAITING_PAYMENT_RESULT)")
        fun create_newPayment() {
            // arrange
            given(paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("pay-key-001")))
                .willReturn(null)
            given(orderRepository.findById(100L)).willReturn(pendingOrder())
            given(paymentRepository.findActiveByOrderId(100L)).willReturn(null)
            given(
                paymentRepository.save(
                    check { payment ->
                        assertThat(payment.status).isEqualTo(Payment.Status.PENDING)
                        assertThat(payment.orderId).isEqualTo(100L)
                    },
                ),
            ).willAnswer { savedPayment(it.arguments[0] as Payment) }

            // act
            val result = useCase.create(command())

            // assert
            assertThat(result).isInstanceOf(PaymentCreateResult.NewlyCreated::class.java)
            assertAll(
                { assertThat(result.result.paymentId).isEqualTo(200L) },
                { assertThat(result.result.status).isEqualTo("PENDING") },
                { assertThat(result.result.displayStatus).isEqualTo("AWAITING_PAYMENT_RESULT") },
                { assertThat(result.result.transactionKey).isNull() },
                { assertThat(result.result.reasonCode).isNull() },
            )
        }
    }

    @Nested
    @DisplayName("동일 멱등키 + 동일 요청 시 IdempotentReplay를 반환한다")
    inner class WhenIdempotentReplay {

        @Test
        @DisplayName("동일 fingerprint → IdempotentReplay(기존 Payment)")
        fun create_idempotentReplay() {
            // arrange
            given(paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("pay-key-001")))
                .willReturn(existingPayment())
            given(orderRepository.findById(100L)).willReturn(pendingOrder())

            // act
            val result = useCase.create(command())

            // assert
            assertThat(result).isInstanceOf(PaymentCreateResult.IdempotentReplay::class.java)
            assertThat(result.result.paymentId).isEqualTo(200L)
        }
    }

    @Nested
    @DisplayName("동일 멱등키 + 다른 요청 시 PAYMENT_IDEMPOTENCY_CONFLICT 예외를 던진다")
    inner class WhenIdempotencyConflict {

        @Test
        @DisplayName("다른 fingerprint → 409 Conflict")
        fun create_conflict() {
            // arrange
            val differentFingerprint = PaymentCreateUseCase.computeFingerprint(999L, "MASTER", "9999")
            given(paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("pay-key-001")))
                .willReturn(existingPayment(fingerprint = differentFingerprint))

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_IDEMPOTENCY_CONFLICT)
        }
    }

    @Nested
    @DisplayName("Order가 없으면 ORDER_NOT_FOUND 예외를 던진다")
    inner class WhenOrderNotFound {

        @Test
        @DisplayName("Order 조회 null → 예외")
        fun create_orderNotFound() {
            // arrange
            given(paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("pay-key-001")))
                .willReturn(null)
            given(orderRepository.findById(100L)).willReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("Order가 PENDING이 아니면 PAYMENT_ORDER_NOT_PENDING 예외를 던진다")
    inner class WhenOrderNotPending {

        @Test
        @DisplayName("Order=CREATED → 예외")
        fun create_orderAlreadyCreated() {
            // arrange
            given(paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("pay-key-001")))
                .willReturn(null)
            given(orderRepository.findById(100L)).willReturn(createdOrder())

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_ORDER_NOT_PENDING)
        }
    }

    @Nested
    @DisplayName("활성 PENDING Payment가 있으면 PAYMENT_ACTIVE_PENDING_EXISTS 예외를 던진다")
    inner class WhenActivePendingExists {

        @Test
        @DisplayName("같은 orderId에 PENDING Payment 존재 → 예외")
        fun create_activePendingExists() {
            // arrange
            given(paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("pay-key-001")))
                .willReturn(null)
            given(orderRepository.findById(100L)).willReturn(pendingOrder())
            given(paymentRepository.findActiveByOrderId(100L)).willReturn(existingPayment())

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_ACTIVE_PENDING_EXISTS)
        }
    }

    @Nested
    @DisplayName("카드번호 마스킹")
    inner class WhenMaskCardNo {

        @Test
        @DisplayName("16자리 카드번호 → 앞 12자리 마스킹, 뒤 4자리 유지")
        fun maskCardNo_standard() {
            assertThat(PaymentCreateUseCase.maskCardNo("4111111111111234")).isEqualTo("************1234")
        }

        @Test
        @DisplayName("4자리 이하 → 전체 마스킹")
        fun maskCardNo_short() {
            assertThat(PaymentCreateUseCase.maskCardNo("1234")).isEqualTo("****")
        }
    }
}
