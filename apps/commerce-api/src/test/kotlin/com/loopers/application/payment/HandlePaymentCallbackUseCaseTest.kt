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

class HandlePaymentCallbackUseCaseTest {

    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var useCase: HandlePaymentCallbackUseCase

    @BeforeEach
    fun setUp() {
        paymentRepository = FakePaymentRepository()
        orderRepository = FakeOrderRepository()
        useCase = HandlePaymentCallbackUseCase(paymentRepository, orderRepository)
    }

    private fun createPendingOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(1),
            ),
        )
        val saved = orderRepository.save(order)
        saved.markPendingPayment()
        orderRepository.save(saved)
        return saved
    }

    private fun createPaymentForOrder(orderId: Long): Payment {
        val payment = Payment.create(
            orderId = orderId,
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 10000L,
        )
        return paymentRepository.save(payment)
    }

    @Nested
    @DisplayName("SUCCESS 콜백 처리 시")
    inner class SuccessCallback {

        @Test
        @DisplayName("Payment가 SUCCESS, Order가 PAID 상태로 전환된다")
        fun handleCallback_success_updatesPaymentAndOrder() {
            // arrange
            val order = createPendingOrder()
            createPaymentForOrder(order.id.value)

            // act
            useCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = order.id.value,
                    transactionKey = "TR-001",
                    success = true,
                ),
            )

            // assert
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }
    }

    @Nested
    @DisplayName("FAILED 콜백 처리 시")
    inner class FailedCallback {

        @Test
        @DisplayName("Payment가 FAILED, Order가 FAILED 상태로 전환된다")
        fun handleCallback_failed_updatesPaymentAndOrder() {
            // arrange
            val order = createPendingOrder()
            createPaymentForOrder(order.id.value)

            // act
            useCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = order.id.value,
                    transactionKey = "TR-001",
                    success = false,
                    reason = "잔액 부족",
                ),
            )

            // assert
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.FAILED)
        }
    }

    @Nested
    @DisplayName("이미 처리된 결제에 콜백이 도착할 시")
    inner class IdempotentCallback {

        @Test
        @DisplayName("이미 SUCCESS 상태인 Payment는 Order 상태를 변경하지 않고 무시된다")
        fun handleCallback_alreadyProcessed_isIgnored() {
            // arrange
            val order = createPendingOrder()
            val payment = createPaymentForOrder(order.id.value)
            // Payment를 SUCCESS로 만들어 둠
            paymentRepository.updateStatusConditionally(
                payment.id,
                listOf(PaymentStatus.REQUESTED),
                PaymentStatus.SUCCESS,
            )

            // act — SUCCESS 콜백 재시도
            useCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = order.id.value,
                    transactionKey = "TR-001",
                    success = true,
                ),
            )

            // assert — Order 상태는 여전히 PENDING_PAYMENT (변경 없음)
            val orderAfter = orderRepository.findById(order.id)!!
            assertThat(orderAfter.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
        }
    }

    @Nested
    @DisplayName("존재하지 않는 orderId로 콜백이 도착할 시")
    inner class NotFoundOrder {

        @Test
        @DisplayName("NOT_FOUND 예외가 발생한다")
        fun handleCallback_notFoundPayment_throwsNotFound() {
            // arrange — Payment 없음

            // act & assert
            val ex = assertThrows<CoreException> {
                useCase.execute(
                    PaymentCommand.HandleCallback(
                        orderId = 999L,
                        transactionKey = "TR-001",
                        success = true,
                    ),
                )
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
