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
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("PaymentCreateUseCase")
class PaymentCreateUseCaseTest {
    private val paymentKey = "11111111-1111-1111-1111-111111111111"

    private val orderRepository: OrderRepository = mock()
    private val paymentRepository: PaymentRepository = mock()
    private val pgPaymentPort: PgPaymentPort = mock()
    private val paymentRecoveryService: PaymentRecoveryService = mock()
    private val transactionTemplate = object : TransactionTemplate() {
        override fun <T> execute(action: TransactionCallback<T>): T? {
            return action.doInTransaction(mock<TransactionStatus>())
        }
    }
    private val callbackBaseUrl = "http://localhost:8080"
    private val useCase = PaymentCreateUseCase(
        orderRepository,
        paymentRepository,
        pgPaymentPort,
        paymentRecoveryService,
        transactionTemplate,
        callbackBaseUrl,
    )

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    @BeforeEach
    fun commonSetUp() {
        given(pgPaymentPort.isAvailable()).willReturn(true)
    }

    private fun command(
        userId: Long = 1L,
        orderId: Long = 100L,
        idempotencyKey: String = paymentKey,
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
        userId: Long = 1L,
        fingerprint: String = PaymentCreateUseCase.computeFingerprint(100L, "VISA", "4111111111111234"),
    ): Payment = Payment.retrieve(
        id = id,
        orderId = orderId,
        userId = userId,
        idempotencyKey = PaymentIdempotencyKey(paymentKey),
        status = Payment.Status.PENDING,
        cardType = "VISA",
        maskedCardNo = "************1234",
        amount = Money(BigDecimal("16000")),
        transactionKey = null,
        reasonCode = null,
        requestFingerprint = fingerprint,
        createdAt = now,
    )

    private fun stubNewPaymentFlow() {
        given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(pendingOrder())
        given(paymentRepository.findByIdempotencyKeyForUpdate(PaymentIdempotencyKey(paymentKey)))
            .willReturn(null)
        given(paymentRepository.findActiveByOrderIdForUpdate(100L)).willReturn(null)
    }

    private fun stubSaveReturnsWithId() {
        given(
            paymentRepository.save(
                check { payment ->
                    assertThat(payment.orderId).isEqualTo(100L)
                },
            ),
        ).willAnswer {
            val payment = it.arguments[0] as Payment
            if (payment.id == null) {
                Payment.retrieve(
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
            } else {
                payment
            }
        }
    }

    @Nested
    @DisplayName("PG 결제 요청 성공 시 transactionKey가 반영된다")
    inner class WhenPgAccepted {

        @Test
        @DisplayName("PG Accepted -> Payment에 transactionKey 저장, PENDING 유지")
        fun create_pgAccepted() {
            // arrange
            stubNewPaymentFlow()
            stubSaveReturnsWithId()
            given(
                pgPaymentPort.requestPayment(
                    check<PgPaymentRequest> { req ->
                        assertThat(req.orderId).isEqualTo(100L)
                        assertThat(req.callbackUrl).isEqualTo("http://localhost:8080/webhook/v1/payments/200")
                    },
                ),
            ).willReturn(PgPaymentResponse.Accepted("txn-abc-123"))

            // act
            val result = useCase.create(command())

            // assert
            assertThat(result).isInstanceOf(PaymentCreateResult.NewlyCreated::class.java)
            assertAll(
                { assertThat(result.result.paymentId).isEqualTo(200L) },
                { assertThat(result.result.status).isEqualTo("PENDING") },
                { assertThat(result.result.transactionKey).isEqualTo("txn-abc-123") },
                { assertThat(result.result.displayStatus).isEqualTo("AWAITING_PAYMENT_RESULT") },
                { assertThat(result.result.reasonCode).isNull() },
            )
        }
    }

    @Nested
    @DisplayName("PG 즉시 실패 시 Payment가 FAILED로 전이된다")
    inner class WhenPgImmediateFailure {

        @Test
        @DisplayName("PG ImmediateFailure -> Payment=FAILED, reasonCode=PG_INTERNAL_ERROR")
        fun create_pgImmediateFailure() {
            // arrange
            stubNewPaymentFlow()
            stubSaveReturnsWithId()
            given(
                pgPaymentPort.requestPayment(
                    check<PgPaymentRequest> { req ->
                        assertThat(req.orderId).isEqualTo(100L)
                    },
                ),
            ).willReturn(PgPaymentResponse.ImmediateFailure(PaymentReasonCode.PG_INTERNAL_ERROR))

            // act
            val result = useCase.create(command())

            // assert
            assertThat(result).isInstanceOf(PaymentCreateResult.NewlyCreated::class.java)
            assertAll(
                { assertThat(result.result.status).isEqualTo("FAILED") },
                { assertThat(result.result.reasonCode).isEqualTo("PG_INTERNAL_ERROR") },
                { assertThat(result.result.transactionKey).isNull() },
            )
        }
    }

    @Nested
    @DisplayName("PG timeout 시 Payment가 PENDING으로 유지되고 reasonCode=TIMEOUT_UNCERTAIN이 설정된다")
    inner class WhenPgTimeout {

        @Test
        @DisplayName("Timeout -> Payment=PENDING, reasonCode=TIMEOUT_UNCERTAIN, transactionKey=null")
        fun create_pgTimeout() {
            // arrange
            stubNewPaymentFlow()
            stubSaveReturnsWithId()
            given(
                pgPaymentPort.requestPayment(
                    check<PgPaymentRequest> { req ->
                        assertThat(req.orderId).isEqualTo(100L)
                    },
                ),
            ).willReturn(PgPaymentResponse.Timeout)

            // act
            val result = useCase.create(command())

            // assert
            assertThat(result).isInstanceOf(PaymentCreateResult.NewlyCreated::class.java)
            assertAll(
                { assertThat(result.result.status).isEqualTo("PENDING") },
                { assertThat(result.result.reasonCode).isEqualTo("TIMEOUT_UNCERTAIN") },
                { assertThat(result.result.transactionKey).isNull() },
                { assertThat(result.result.displayStatus).isEqualTo("AWAITING_PAYMENT_RESULT") },
            )
        }
    }

    @Nested
    @DisplayName("Circuit Breaker가 OPEN이면 PG_CIRCUIT_OPEN 예외를 던지고 Payment를 생성하지 않는다")
    inner class WhenCircuitOpen {

        @Test
        @DisplayName("isAvailable=false -> 503, Payment/Order 미생성")
        fun create_circuitOpen() {
            // arrange
            given(pgPaymentPort.isAvailable()).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PG_CIRCUIT_OPEN)
            verify(paymentRepository, never()).save(check<Payment> { })
            verify(pgPaymentPort, never()).requestPayment(check<PgPaymentRequest> { })
        }

        @Test
        @DisplayName("isAvailable=true지만 request 시 CircuitOpen -> Payment 보상 삭제 후 503")
        fun create_lateCircuitOpen() {
            // arrange
            stubNewPaymentFlow()
            stubSaveReturnsWithId()
            given(pgPaymentPort.requestPayment(check<PgPaymentRequest> { }))
                .willReturn(PgPaymentResponse.CircuitOpen)

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PG_CIRCUIT_OPEN)
            verify(paymentRepository).hardDelete(200L)
            verify(paymentRecoveryService, never()).scheduleEagerRetry(200L)
        }
    }

    @Nested
    @DisplayName("동일 멱등키 + 동일 요청 시 IdempotentReplay를 반환한다")
    inner class WhenIdempotentReplay {

        @Test
        @DisplayName("동일 fingerprint -> IdempotentReplay(기존 Payment), PG 호출 없음")
        fun create_idempotentReplay() {
            // arrange
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(pendingOrder())
            given(paymentRepository.findByIdempotencyKeyForUpdate(PaymentIdempotencyKey(paymentKey)))
                .willReturn(existingPayment())

            // act
            val result = useCase.create(command())

            // assert
            assertThat(result).isInstanceOf(PaymentCreateResult.IdempotentReplay::class.java)
            assertThat(result.result.paymentId).isEqualTo(200L)
            verify(pgPaymentPort, never()).requestPayment(
                check<PgPaymentRequest> { },
            )
        }
    }

    @Nested
    @DisplayName("동일 멱등키 + 다른 요청 시 PAYMENT_IDEMPOTENCY_CONFLICT 예외를 던진다")
    inner class WhenIdempotencyConflict {

        @Test
        @DisplayName("다른 fingerprint -> 409 Conflict")
        fun create_conflict() {
            // arrange
            val differentFingerprint = PaymentCreateUseCase.computeFingerprint(999L, "MASTER", "9999")
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(pendingOrder())
            given(paymentRepository.findByIdempotencyKeyForUpdate(PaymentIdempotencyKey(paymentKey)))
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
        @DisplayName("Order 조회 null -> 예외")
        fun create_orderNotFound() {
            // arrange
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_NOT_FOUND)
        }

        @Test
        @DisplayName("타인 주문 조회 null -> ORDER_NOT_FOUND")
        fun create_orderNotOwned() {
            // arrange
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 2L)).willReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command(userId = 2L))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("Order가 PENDING이 아니면 PAYMENT_ORDER_NOT_PENDING 예외를 던진다")
    inner class WhenOrderNotPending {

        @Test
        @DisplayName("Order=CREATED -> 예외")
        fun create_orderAlreadyCreated() {
            // arrange
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(createdOrder())

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
        @DisplayName("같은 orderId에 PENDING Payment 존재 -> 예외")
        fun create_activePendingExists() {
            // arrange
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(pendingOrder())
            given(paymentRepository.findByIdempotencyKeyForUpdate(PaymentIdempotencyKey(paymentKey)))
                .willReturn(null)
            given(paymentRepository.findActiveByOrderIdForUpdate(100L)).willReturn(existingPayment())

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_ACTIVE_PENDING_EXISTS)
        }
    }

    @Nested
    @DisplayName("멱등키 소유자가 다르면 PAYMENT_IDEMPOTENCY_CONFLICT 예외를 던진다")
    inner class WhenForeignUserIdempotencyKey {

        @Test
        @DisplayName("기존 Payment userId != 요청 userId -> 409 Conflict")
        fun create_foreignUserIdempotencyKey() {
            // arrange
            given(orderRepository.findByIdAndUserIdForUpdate(100L, 1L)).willReturn(pendingOrder())
            given(paymentRepository.findByIdempotencyKeyForUpdate(PaymentIdempotencyKey(paymentKey)))
                .willReturn(existingPayment(userId = 999L))

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_IDEMPOTENCY_CONFLICT)
        }
    }

    @Nested
    @DisplayName("UUID 형식이 아닌 멱등키는 PAYMENT_INVALID_IDEMPOTENCY_KEY 예외를 던진다")
    inner class WhenInvalidIdempotencyKey {

        @Test
        @DisplayName("invalid uuid -> 400 Bad Request")
        fun create_invalidIdempotencyKey() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command(idempotencyKey = "not-a-uuid"))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }
    }

    @Nested
    @DisplayName("카드번호 마스킹")
    inner class WhenMaskCardNo {

        @Test
        @DisplayName("16자리 카드번호 -> 앞 12자리 마스킹, 뒤 4자리 유지")
        fun maskCardNo_standard() {
            assertThat(PaymentCreateUseCase.maskCardNo("4111111111111234")).isEqualTo("************1234")
        }

        @Test
        @DisplayName("4자리 이하 -> 전체 마스킹")
        fun maskCardNo_short() {
            assertThat(PaymentCreateUseCase.maskCardNo("1234")).isEqualTo("****")
        }
    }
}
