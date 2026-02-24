package com.loopers.domain.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    @Captor
    private lateinit var orderCaptor: ArgumentCaptor<Order>

    @Captor
    private lateinit var orderItemsCaptor: ArgumentCaptor<List<OrderItem>>

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, orderItemRepository)
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("Order가 저장된다.")
        @Test
        fun savesOrder() {
            // arrange
            val userId = 1L
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    quantity = 2,
                    productName = "에어맥스",
                    productPrice = 159000L,
                    brandName = "나이키",
                ),
            )
            val savedOrder = Order(userId = userId, totalAmount = 318000L)
            whenever(orderRepository.save(any())).thenReturn(savedOrder)
            whenever(orderItemRepository.saveAll(any())).thenReturn(emptyList())

            // act
            val result = orderService.createOrder(userId, items)

            // assert
            verify(orderRepository).save(capture(orderCaptor))
            val capturedOrder = orderCaptor.value
            assertAll(
                { assertThat(capturedOrder.userId).isEqualTo(userId) },
                { assertThat(capturedOrder.totalAmount).isEqualTo(318000L) },
                { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @DisplayName("OrderItem이 저장된다.")
        @Test
        fun savesOrderItems() {
            // arrange
            val userId = 1L
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    quantity = 2,
                    productName = "에어맥스",
                    productPrice = 159000L,
                    brandName = "나이키",
                ),
                OrderItemCommand(
                    productId = 2L,
                    quantity = 1,
                    productName = "에어포스",
                    productPrice = 139000L,
                    brandName = "나이키",
                ),
            )
            val savedOrder = Order(userId = userId, totalAmount = 457000L)
            whenever(orderRepository.save(any())).thenReturn(savedOrder)
            whenever(orderItemRepository.saveAll(any())).thenReturn(emptyList())

            // act
            orderService.createOrder(userId, items)

            // assert
            verify(orderItemRepository).saveAll(capture(orderItemsCaptor))
            assertThat(orderItemsCaptor.value).hasSize(2)
        }

        @DisplayName("총 금액은 각 항목의 (가격 x 수량) 합계이다.")
        @Test
        fun calculatesTotalAmount() {
            // arrange
            val userId = 1L
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    quantity = 2,
                    productName = "에어맥스",
                    productPrice = 159000L,
                    brandName = "나이키",
                ),
                OrderItemCommand(
                    productId = 2L,
                    quantity = 3,
                    productName = "에어포스",
                    productPrice = 139000L,
                    brandName = "나이키",
                ),
            )
            // 159000 * 2 + 139000 * 3 = 318000 + 417000 = 735000
            val savedOrder = Order(userId = userId, totalAmount = 735000L)
            whenever(orderRepository.save(any())).thenReturn(savedOrder)
            whenever(orderItemRepository.saveAll(any())).thenReturn(emptyList())

            // act
            val result = orderService.createOrder(userId, items)

            // assert
            verify(orderRepository).save(capture(orderCaptor))
            assertThat(orderCaptor.value.totalAmount).isEqualTo(735000L)
            assertThat(result.totalAmount).isEqualTo(735000L)
        }
    }

    @DisplayName("주문 목록을 조회할 때,")
    @Nested
    inner class GetOrders {

        @DisplayName("Repository에서 조회한 결과를 반환한다.")
        @Test
        fun returnsOrdersFromRepository() {
            // arrange
            val userId = 1L
            val startAt = LocalDateTime.of(2026, 2, 1, 0, 0)
            val endAt = LocalDateTime.of(2026, 2, 28, 23, 59, 59)
            val expectedOrders = listOf(Order(userId = userId, totalAmount = 159000L))
            whenever(orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt))
                .thenReturn(expectedOrders)

            // act
            val result = orderService.getOrders(userId, startAt, endAt)

            // assert
            assertThat(result).hasSize(1)
            verify(orderRepository).findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
        }
    }

    @DisplayName("주문을 단건 조회할 때,")
    @Nested
    inner class GetOrder {

        @DisplayName("존재하는 주문이면, 주문을 반환한다.")
        @Test
        fun returnsOrder_whenOrderExists() {
            // arrange
            val orderId = 1L
            val expectedOrder = Order(userId = 1L, totalAmount = 318000L)
            whenever(orderRepository.findById(orderId)).thenReturn(expectedOrder)

            // act
            val result = orderService.getOrder(orderId)

            // assert
            assertAll(
                { assertThat(result.userId).isEqualTo(1L) },
                { assertThat(result.totalAmount).isEqualTo(318000L) },
                { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
            )
            verify(orderRepository).findById(orderId)
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외를 던진다.")
        @Test
        fun throwsNotFound_whenOrderDoesNotExist() {
            // arrange
            val orderId = 999L
            whenever(orderRepository.findById(orderId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(orderId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
