package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Captor
    private lateinit var orderCaptor: ArgumentCaptor<Order>

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository)
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("Order가 항목과 함께 저장된다.")
        @Test
        fun savesOrderWithItems() {
            // arrange
            val userId = 1L
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    quantity = Quantity.of(2),
                    productName = "에어맥스",
                    productPrice = Money.of(159000L),
                    brandName = "나이키",
                ),
            )
            whenever(orderRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = orderService.createOrder(userId, items)

            // assert
            verify(orderRepository).save(capture(orderCaptor))
            val capturedOrder = orderCaptor.value
            assertAll(
                { assertThat(capturedOrder.userId).isEqualTo(userId) },
                { assertThat(capturedOrder.totalAmount).isEqualTo(Money.of(318000L)) },
                { assertThat(capturedOrder.items).hasSize(1) },
                { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @DisplayName("총 금액은 각 항목의 (가격 x 수량) 합계이다.")
        @Test
        fun calculatesTotalAmount() {
            // arrange
            val userId = 1L
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    quantity = Quantity.of(2),
                    productName = "에어맥스",
                    productPrice = Money.of(159000L),
                    brandName = "나이키",
                ),
                OrderItemCommand(
                    productId = 2L,
                    quantity = Quantity.of(3),
                    productName = "에어포스",
                    productPrice = Money.of(139000L),
                    brandName = "나이키",
                ),
            )
            // 159000 * 2 + 139000 * 3 = 318000 + 417000 = 735000
            whenever(orderRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = orderService.createOrder(userId, items)

            // assert
            verify(orderRepository).save(capture(orderCaptor))
            assertThat(orderCaptor.value.totalAmount).isEqualTo(Money.of(735000L))
            assertThat(result.totalAmount).isEqualTo(Money.of(735000L))
        }

        @DisplayName("주문 항목이 비어있으면, BAD_REQUEST 예외를 던진다.")
        @Test
        fun throwsBadRequest_whenItemsEmpty() {
            // act
            val exception = assertThrows<CoreException> {
                orderService.createOrder(1L, emptyList())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문 수량이 0 이하이면, Quantity 생성 시 BAD_REQUEST 예외를 던진다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZeroOrNegative() {
            // act
            val exception = assertThrows<CoreException> {
                Quantity.of(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("중복된 상품이 포함되면, BAD_REQUEST 예외를 던진다.")
        @Test
        fun throwsBadRequest_whenDuplicateProductIds() {
            // arrange
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    quantity = Quantity.of(1),
                    productName = "에어맥스",
                    productPrice = Money.of(159000L),
                    brandName = "나이키",
                ),
                OrderItemCommand(
                    productId = 1L,
                    quantity = Quantity.of(2),
                    productName = "에어맥스",
                    productPrice = Money.of(159000L),
                    brandName = "나이키",
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderService.createOrder(1L, items)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
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
            val expectedOrders = listOf(Order(userId = userId))
            whenever(orderRepository.findByUserIdAndCreatedAtBetween(userId, startAt, endAt))
                .thenReturn(expectedOrders)

            // act
            val result = orderService.getOrders(userId, startAt, endAt)

            // assert
            assertThat(result).hasSize(1)
            verify(orderRepository).findByUserIdAndCreatedAtBetween(userId, startAt, endAt)
        }

        @DisplayName("시작일이 종료일보다 크면, BAD_REQUEST 예외를 던진다.")
        @Test
        fun throwsBadRequest_whenStartAtIsAfterEndAt() {
            // arrange
            val userId = 1L
            val startAt = LocalDateTime.of(2026, 2, 28, 23, 59, 59)
            val endAt = LocalDateTime.of(2026, 2, 1, 0, 0)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrders(userId, startAt, endAt)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문을 단건 조회할 때,")
    @Nested
    inner class GetOrder {

        @DisplayName("존재하는 주문이면, 주문을 반환한다.")
        @Test
        fun returnsOrder_whenOrderExists() {
            // arrange
            val userId = 1L
            val orderId = 1L
            val expectedOrder = Order(userId = userId)
            whenever(orderRepository.findById(orderId)).thenReturn(expectedOrder)

            // act
            val result = orderService.getOrder(userId, orderId)

            // assert
            assertAll(
                { assertThat(result.userId).isEqualTo(userId) },
                { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
            )
            verify(orderRepository).findById(orderId)
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외를 던진다.")
        @Test
        fun throwsNotFound_whenOrderDoesNotExist() {
            // arrange
            val userId = 1L
            val orderId = 999L
            whenever(orderRepository.findById(orderId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(userId, orderId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("다른 사용자의 주문이면, FORBIDDEN 예외를 던진다.")
        @Test
        fun throwsForbidden_whenOtherUsersOrder() {
            // arrange
            val orderId = 1L
            val orderOwnerUserId = 1L
            val requestUserId = 2L
            val expectedOrder = Order(userId = orderOwnerUserId)
            whenever(orderRepository.findById(orderId)).thenReturn(expectedOrder)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(requestUserId, orderId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
