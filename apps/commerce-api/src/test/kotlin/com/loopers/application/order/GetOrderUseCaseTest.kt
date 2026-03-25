package com.loopers.application.order

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.common.vo.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class GetOrderUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var getOrderUseCase: GetOrderUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        paymentRepository = FakePaymentRepository()
        getOrderUseCase = GetOrderUseCase(orderRepository, orderItemRepository, paymentRepository)
    }

    private fun createAndSaveOrder(userId: Long): Pair<Order, List<OrderItem>> {
        val order = Order.create(
            UserId(userId),
            listOf(OrderProductData(ProductId(1), "테스트 상품", Money(BigDecimal("10000"))) to Quantity(1)),
        )
        val savedOrder = orderRepository.save(order)
        order.assignOrderIdToItems(savedOrder.id)
        val savedItems = order.items.map { orderItemRepository.save(it) }
        return savedOrder to savedItems
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("주문 소유자가 조회하면 OrderInfo가 반환된다")
        fun execute_owner_returnsOrderInfo() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.id).isEqualTo(order.id.value)
            assertThat(result.userId).isEqualTo(1L)
            assertThat(result.items).hasSize(1)
        }

        @Test
        @DisplayName("다른 사용자가 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_otherUser_throwsNotFound() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)

            // act
            val exception = assertThrows<CoreException> {
                getOrderUseCase.execute(2L, order.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_deletedOrder_throwsNotFound() {
            // arrange
            val deletedOrder = Order.fromPersistence(
                id = OrderId(0),
                refUserId = UserId(1),
                status = Order.OrderStatus.CREATED,
                originalPrice = Money(BigDecimal("10000")),
                discountAmount = Money(BigDecimal.ZERO),
                totalPrice = Money(BigDecimal("10000")),
                refCouponId = null,
                deletedAt = ZonedDateTime.now(),
            )
            val saved = orderRepository.save(deletedOrder)

            // act
            val exception = assertThrows<CoreException> {
                getOrderUseCase.execute(1L, saved.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getOrderUseCase.execute(1L, 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("결제 완료된 주문 조회 시 결제 정보가 포함된다")
        fun execute_withPayment_returnsPaymentInfo() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)
            val payment = Payment.create(
                orderId = order.id.value,
                cardType = CardType.KB,
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
            )
            payment.markSuccess("txn-key-123")
            paymentRepository.save(payment)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(result.transactionKey).isEqualTo("txn-key-123")
        }

        @Test
        @DisplayName("결제 정보가 없는 주문 조회 시 결제 필드가 null이다")
        fun execute_withoutPayment_paymentFieldsNull() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.paymentStatus).isNull()
            assertThat(result.transactionKey).isNull()
        }

        @Test
        @DisplayName("결제 실패 주문 조회 시 실패 상태가 반환된다")
        fun execute_withFailedPayment_returnsFailedStatus() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)
            val payment = Payment.create(
                orderId = order.id.value,
                cardType = CardType.SAMSUNG,
                cardNo = "9999-8888-7777-6666",
                amount = 10000L,
            )
            payment.markFailed("잔액 부족")
            paymentRepository.save(payment)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.FAILED)
            assertThat(result.transactionKey).isNull()
        }

        @Test
        @DisplayName("결제 요청 중인 주문 조회 시 REQUESTED 상태가 반환된다")
        fun execute_withRequestedPayment_returnsRequestedStatus() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)
            val payment = Payment.create(
                orderId = order.id.value,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
            )
            paymentRepository.save(payment)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.REQUESTED)
            assertThat(result.transactionKey).isNull()
        }

        @Test
        @DisplayName("결제 타임아웃 주문 조회 시 TIMEOUT 상태가 반환된다")
        fun execute_withTimeoutPayment_returnsTimeoutStatus() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)
            val payment = Payment.create(
                orderId = order.id.value,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
            )
            payment.markTimeout()
            paymentRepository.save(payment)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.TIMEOUT)
            assertThat(result.transactionKey).isNull()
        }
    }
}
