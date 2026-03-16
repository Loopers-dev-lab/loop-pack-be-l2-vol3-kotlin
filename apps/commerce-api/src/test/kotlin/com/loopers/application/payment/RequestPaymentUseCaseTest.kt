package com.loopers.application.payment

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.payment.FakePgClient
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.PgPaymentResult
import com.loopers.domain.payment.PgResultStatus
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
    private lateinit var pgClient: FakePgClient
    private lateinit var useCase: RequestPaymentUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        paymentRepository = FakePaymentRepository()
        pgClient = FakePgClient()
        useCase = RequestPaymentUseCase(orderRepository, paymentRepository, pgClient)
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

    private fun defaultCommand(orderId: Long) = PaymentCommand.RequestPayment(
        orderId = orderId,
        cardType = "SAMSUNG",
        cardNo = "1234-5678-9012-3456",
        callbackUrl = "https://example.com/callback",
    )

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("PG 성공 — Payment가 REQUESTED 상태로 저장되고 Order가 PENDING_PAYMENT로 전환된다")
        fun execute_pgSuccess_paymentRequestedAndOrderPendingPayment() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = "TR-SUCCESS-001",
                status = PgResultStatus.SUCCESS,
            )
            val command = defaultCommand(savedOrder.id.value)

            // act
            val result = useCase.execute(command)

            // assert
            assertThat(result.status).isEqualTo(PaymentStatus.REQUESTED.name)
            assertThat(result.orderId).isEqualTo(savedOrder.id.value)
            assertThat(result.cardType).isEqualTo("SAMSUNG")
            assertThat(result.amount).isEqualTo(20000L)
            assertThat(result.id).isNotEqualTo(0L)

            val updatedOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)

            val savedPayment = paymentRepository.findByOrderId(savedOrder.id.value)
            assertThat(savedPayment).isNotNull
        }

        @Test
        @DisplayName("PG TIMEOUT — Payment가 TIMEOUT 상태로 저장되고 Order는 PENDING_PAYMENT로 남는다")
        fun execute_pgTimeout_paymentTimeoutAndOrderPendingPayment() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.TIMEOUT,
            )
            val command = defaultCommand(savedOrder.id.value)

            // act
            val result = useCase.execute(command)

            // assert
            assertThat(result.status).isEqualTo(PaymentStatus.TIMEOUT.name)

            val updatedOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
        }

        @Test
        @DisplayName("PG FAILED — Payment가 FAILED 상태로 저장되고 Order가 FAILED로 전환된다")
        fun execute_pgFailed_paymentFailedAndOrderFailed() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.FAILED,
                reason = "카드 한도 초과",
            )
            val command = defaultCommand(savedOrder.id.value)

            // act
            val result = useCase.execute(command)

            // assert
            assertThat(result.status).isEqualTo(PaymentStatus.FAILED.name)
            assertThat(result.reason).isEqualTo("카드 한도 초과")

            val updatedOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.FAILED)
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
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = "TR-FIRST-001",
                status = PgResultStatus.SUCCESS,
            )
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
