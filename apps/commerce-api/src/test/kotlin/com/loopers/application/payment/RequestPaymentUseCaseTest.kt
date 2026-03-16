package com.loopers.application.payment

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class RequestPaymentUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var paymentPgProcessor: FakePaymentPgProcessor
    private lateinit var useCase: RequestPaymentUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        paymentRepository = FakePaymentRepository()
        paymentPgProcessor = FakePaymentPgProcessor()
        useCase = RequestPaymentUseCase(orderRepository, paymentRepository, paymentPgProcessor)
    }

    private fun createSavedOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(
                    id = ProductId(1L),
                    name = "상품A",
                    price = Money(BigDecimal("10000")),
                ) to Quantity(2),
            ),
        )
        return orderRepository.save(order)
    }

    private fun defaultCommand(orderId: Long, userId: Long = 1L) = PaymentCommand.RequestPayment(
        userId = userId,
        orderId = orderId,
        cardType = "SAMSUNG",
        cardNo = "1234-5678-9012-3456",
    )

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("결제 요청 시 Payment가 REQUESTED 상태로 저장되고 Order가 PENDING_PAYMENT로 전환된다")
        fun execute_paymentRequestedAndOrderPendingPayment() {
            // arrange
            val savedOrder = createSavedOrder()
            val command = defaultCommand(savedOrder.id.value)

            // act
            val result = useCase.execute(command)

            // assert — execute()는 항상 REQUESTED 상태 반환 (PG 호출은 afterCommit에서)
            assertThat(result.status).isEqualTo(PaymentStatus.REQUESTED.name)
            assertThat(result.orderId).isEqualTo(savedOrder.id.value)
            assertThat(result.cardType).isEqualTo("SAMSUNG")
            assertThat(result.cardNo).isEqualTo("1234-****-****-3456")
            assertThat(result.amount).isEqualTo(20000L)
            assertThat(result.id).isNotEqualTo(0L)

            val updatedOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)

            val savedPayment = paymentRepository.findByOrderId(savedOrder.id.value)
            assertThat(savedPayment).isNotNull
        }

        @Test
        @DisplayName("존재하지 않는 Order ID — NOT_FOUND 예외가 발생한다")
        fun execute_orderNotFound_throwsNotFound() {
            // arrange
            val command = defaultCommand(orderId = 999L)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("REQUESTED 상태 결제가 이미 존재하면 CONFLICT 예외가 발생한다")
        fun execute_alreadyRequestedPayment_throwsConflict() {
            // arrange
            val savedOrder = createSavedOrder()
            val command = defaultCommand(savedOrder.id.value)
            useCase.execute(command) // 첫 번째 결제 요청 (REQUESTED 상태로 저장)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(command) // 동일 주문 재요청
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        @DisplayName("다른 userId의 Order에 결제 요청 시 NOT_FOUND 예외가 발생한다")
        fun execute_orderOwnerMismatch_throwsNotFound() {
            // arrange
            val savedOrder = createSavedOrder() // userId = 1L 로 생성
            val command = defaultCommand(orderId = savedOrder.id.value, userId = 2L) // 다른 userId

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("SUCCESS 상태 결제가 이미 존재하면 CONFLICT 예외가 발생한다")
        fun execute_alreadySuccessPayment_throwsConflict() {
            // arrange
            val savedOrder = createSavedOrder()
            val successPayment = Payment.fromPersistence(
                id = 0L,
                orderId = savedOrder.id.value,
                transactionKey = "TR-001",
                status = PaymentStatus.SUCCESS,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-****-****-3456",
                amount = 20000L,
                reason = null,
                createdAt = java.time.ZonedDateTime.now(),
                updatedAt = java.time.ZonedDateTime.now(),
            )
            paymentRepository.save(successPayment)
            val command = defaultCommand(savedOrder.id.value)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
            assertThat(exception.message).contains("이미 결제가 완료된 주문입니다.")
        }

        @Test
        @DisplayName("FAILED 상태 결제가 존재하면 재결제가 허용된다")
        fun execute_failedPaymentExists_allowsRetry() {
            // arrange
            val savedOrder = createSavedOrder()
            val failedPayment = Payment.fromPersistence(
                id = 0L,
                orderId = savedOrder.id.value,
                transactionKey = null,
                status = PaymentStatus.FAILED,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-****-****-3456",
                amount = 20000L,
                reason = "카드 한도 초과",
                createdAt = java.time.ZonedDateTime.now(),
                updatedAt = java.time.ZonedDateTime.now(),
            )
            paymentRepository.save(failedPayment)
            // CREATED 상태로 되돌려야 markPendingPayment() 가능 — 새 주문으로 테스트
            val freshOrder = createSavedOrder()
            val command = defaultCommand(freshOrder.id.value)

            // act & assert — 예외 없이 성공
            val result = useCase.execute(command)
            assertThat(result.status).isEqualTo(PaymentStatus.REQUESTED.name)
        }

        @Test
        @DisplayName("CREATED 상태가 아닌 Order — BAD_REQUEST 예외가 발생한다")
        fun execute_orderNotCreated_throwsBadRequest() {
            // arrange
            val savedOrder = createSavedOrder()
            // PENDING_PAYMENT 상태로 전환
            savedOrder.markPendingPayment()
            orderRepository.save(savedOrder)
            val command = defaultCommand(savedOrder.id.value)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
