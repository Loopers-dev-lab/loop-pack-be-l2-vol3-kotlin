package com.loopers.application.payment

import com.loopers.application.event.PaymentEvent
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderItemRepository
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
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal

class HandlePaymentCallbackUseCaseTest {

    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var useCase: HandlePaymentCallbackUseCase

    @BeforeEach
    fun setUp() {
        paymentRepository = FakePaymentRepository()
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        useCase = HandlePaymentCallbackUseCase(
            paymentRepository, orderRepository, orderItemRepository, ApplicationEventPublisher { },
        )
    }

    private fun createPendingOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(1),
            ),
        )
        val saved = orderRepository.save(order)
        saved.assignOrderIdToItems(saved.id)
        orderItemRepository.saveAll(saved.items)
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
            val updatedPayment = paymentRepository.findByOrderId(order.id)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }

        @Test
        @DisplayName("PaymentEvent.Completed 이벤트가 발행된다")
        fun handleCallback_success_publishesCompletedEvent() {
            // arrange
            val order = createPendingOrder()
            createPaymentForOrder(order.id.value)
            val publishedEvents = mutableListOf<Any>()
            val trackingUseCase = HandlePaymentCallbackUseCase(
                paymentRepository,
                orderRepository,
                orderItemRepository,
                ApplicationEventPublisher { publishedEvents.add(it) },
            )

            // act
            trackingUseCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = order.id.value,
                    transactionKey = "TR-001",
                    success = true,
                ),
            )

            // assert
            val completedEvents = publishedEvents.filterIsInstance<PaymentEvent.Completed>()
            assertThat(completedEvents).hasSize(1)
            assertThat(completedEvents[0].orderId).isEqualTo(order.id.value)
            assertThat(completedEvents[0].userId).isEqualTo(order.refUserId.value)
            assertThat(completedEvents[0].totalAmount).isEqualTo(10000L)
            assertThat(completedEvents[0].items).hasSize(1)
            assertThat(completedEvents[0].items[0].productId).isEqualTo(1L)
            assertThat(completedEvents[0].items[0].quantity).isEqualTo(1)
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
            val updatedPayment = paymentRepository.findByOrderId(order.id)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(updatedPayment.reason).isEqualTo("잔액 부족")
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.FAILED)
        }

        @Test
        @DisplayName("PaymentEvent.Failed 이벤트가 발행된다")
        fun handleCallback_failed_publishesFailedEvent() {
            // arrange
            val order = createPendingOrder()
            createPaymentForOrder(order.id.value)
            val publishedEvents = mutableListOf<Any>()
            val trackingUseCase = HandlePaymentCallbackUseCase(
                paymentRepository,
                orderRepository,
                orderItemRepository,
                ApplicationEventPublisher { publishedEvents.add(it) },
            )

            // act
            trackingUseCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = order.id.value,
                    transactionKey = "TR-001",
                    success = false,
                    reason = "잔액 부족",
                ),
            )

            // assert
            val failedEvents = publishedEvents.filterIsInstance<PaymentEvent.Failed>()
            assertThat(failedEvents).hasSize(1)
            assertThat(failedEvents[0].orderId).isEqualTo(order.id.value)
            assertThat(failedEvents[0].userId).isEqualTo(order.refUserId.value)
            assertThat(failedEvents[0].reason).isEqualTo("잔액 부족")
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
            // Payment=SUCCESS, Order=PAID 현실적 조합으로 구성
            payment.markSuccess("TR-PRE")
            paymentRepository.save(payment)
            order.markPaid()
            orderRepository.save(order)

            // act — SUCCESS 콜백 재시도
            useCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = order.id.value,
                    transactionKey = "TR-001",
                    success = true,
                ),
            )

            // assert — Order 상태는 여전히 PAID (변경 없음)
            val orderAfter = orderRepository.findById(order.id)!!
            assertThat(orderAfter.status).isEqualTo(Order.OrderStatus.PAID)
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

    @Nested
    @DisplayName("Payment는 존재하지만 Order가 없을 시")
    inner class PaymentExistsButOrderNotFound {

        @Test
        @DisplayName("NOT_FOUND 예외가 발생한다")
        fun handleCallback_paymentExistsButOrderMissing_throwsNotFound() {
            // arrange — Payment만 존재, Order 없음
            createPaymentForOrder(999L)

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

    @Nested
    @DisplayName("transactionKey가 불일치할 시")
    inner class TransactionKeyMismatch {

        @Test
        @DisplayName("BAD_REQUEST 예외가 발생한다")
        fun handleCallback_transactionKeyMismatch_throwsBadRequest() {
            // arrange
            val order = createPendingOrder()
            val payment = Payment.fromPersistence(
                id = 0L,
                orderId = order.id.value,
                transactionKey = "TR-ORIGINAL",
                status = PaymentStatus.REQUESTED,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-****-****-3456",
                amount = 10000L,
                reason = null,
                createdAt = java.time.ZonedDateTime.now(),
                updatedAt = java.time.ZonedDateTime.now(),
            )
            paymentRepository.save(payment)

            // act & assert
            val ex = assertThrows<CoreException> {
                useCase.execute(
                    PaymentCommand.HandleCallback(
                        orderId = order.id.value,
                        transactionKey = "TR-DIFFERENT",
                        success = true,
                    ),
                )
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
