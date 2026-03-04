package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderDomainService
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderDomainService: OrderDomainService

    @InjectMocks
    private lateinit var orderService: OrderService

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("DomainService에 조립을 위임하고, 결과를 저장한다.")
        @Test
        fun delegatesToDomainServiceAndSavesOrder() {
            // arrange
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    productName = "에어맥스 90",
                    brandName = "나이키",
                    quantity = 2,
                    unitPrice = BigDecimal("129000"),
                ),
            )
            val order = Order(userId = 1L)

            whenever(orderDomainService.buildOrder(eq(1L), any(), anyOrNull())).thenReturn(order)
            whenever(orderRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = orderService.createOrder(1L, items)

            // assert
            verify(orderDomainService).buildOrder(eq(1L), any(), anyOrNull())
            verify(orderRepository).save(order)
            assertThat(result.userId).isEqualTo(1L)
        }
    }

    @DisplayName("주문을 조회할 때,")
    @Nested
    inner class GetOrder {

        @DisplayName("존재하는 주문이면, 주문이 반환된다.")
        @Test
        fun returnsOrder_whenOrderExists() {
            // arrange
            val order = Order(userId = 1L)
            whenever(orderRepository.findById(1L)).thenReturn(order)

            // act
            val result = orderService.getOrder(1L)

            // assert
            assertThat(result.userId).isEqualTo(1L)
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // arrange
            whenever(orderRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("유저의 주문 목록을 조회할 때,")
    @Nested
    inner class GetUserOrders {

        @DisplayName("기간 내 주문이 있으면, 주문 목록이 반환된다.")
        @Test
        fun returnsOrders_whenOrdersExistInPeriod() {
            // arrange
            val userId = 1L
            val now = ZonedDateTime.now()
            val startAt = now.minusDays(7)
            val endAt = now
            val order1 = Order(userId = userId)
            ReflectionTestUtils.setField(order1, "createdAt", now)
            val order2 = Order(userId = userId)
            ReflectionTestUtils.setField(order2, "createdAt", now)
            val orders = listOf(order1, order2)

            whenever(orderRepository.findAllByUserId(userId, startAt, endAt)).thenReturn(orders)

            // act
            val result = orderService.getUserOrders(userId, startAt, endAt)

            // assert
            assertThat(result).hasSize(2)
        }
    }

    @DisplayName("전체 주문 목록을 조회할 때,")
    @Nested
    inner class GetAllOrders {

        @DisplayName("페이징된 주문 목록이 반환된다.")
        @Test
        fun returnsPagedOrders() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val now = ZonedDateTime.now()
            val order1 = Order(userId = 1L)
            ReflectionTestUtils.setField(order1, "createdAt", now)
            val order2 = Order(userId = 2L)
            ReflectionTestUtils.setField(order2, "createdAt", now)
            val orders = listOf(order1, order2)
            val page = PageImpl(orders, pageable, 2L)

            whenever(orderRepository.findAll(pageable)).thenReturn(page)

            // act
            val result = orderService.getAllOrders(pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2L)
        }
    }
}
