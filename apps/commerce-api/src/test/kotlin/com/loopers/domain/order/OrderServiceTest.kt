package com.loopers.domain.order

import com.loopers.domain.order.entity.Order
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import java.math.BigDecimal

class OrderServiceTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderService = OrderService(orderRepository)
    }

    private fun createProductInfo(id: Long, name: String, price: BigDecimal): OrderProductInfo {
        return OrderProductInfo(id = id, name = name, price = price)
    }

    private fun createAndSaveOrder(userId: Long): Order {
        val productInfo = createProductInfo(1L, "테스트 상품", BigDecimal("10000"))
        val command = OrderCommand.CreateOrder(
            items = listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = 1)),
        )
        return orderService.createOrder(userId, listOf(productInfo), command)
    }

    @Nested
    @DisplayName("createOrder 시")
    inner class CreateOrder {

        @Test
        @DisplayName("주문이 저장되고 ID가 부여된다")
        fun createOrder_savesAndAssignsId() {
            // arrange
            val productInfo = createProductInfo(1L, "에어맥스 90", BigDecimal("129000"))
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = 2)),
            )

            // act
            val saved = orderService.createOrder(1L, listOf(productInfo), command)

            // assert
            assertThat(saved.id).isNotEqualTo(0L)
            assertThat(saved.refUserId).isEqualTo(1L)
            assertThat(saved.totalPrice).isEqualByComparingTo(BigDecimal("258000"))
        }
    }

    @Nested
    @DisplayName("getOrder 시")
    inner class GetOrder {

        @Test
        @DisplayName("주문 소유자가 조회하면 주문이 반환된다")
        fun getOrder_owner_returnsOrder() {
            // arrange
            val saved = createAndSaveOrder(1L)

            // act
            val result = orderService.getOrder(1L, saved.id)

            // assert
            assertThat(result.id).isEqualTo(saved.id)
            assertThat(result.refUserId).isEqualTo(1L)
        }

        @Test
        @DisplayName("다른 사용자가 조회하면 NOT_FOUND 예외가 발생한다")
        fun getOrder_otherUser_throwsNotFound() {
            // arrange
            val saved = createAndSaveOrder(1L)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(2L, saved.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getOrder_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(1L, 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("getOrderForAdmin 시")
    inner class GetOrderForAdmin {

        @Test
        @DisplayName("주문이 존재하면 소유자와 관계없이 반환된다")
        fun getOrderForAdmin_existingOrder_returnsOrder() {
            // arrange
            val saved = createAndSaveOrder(1L)

            // act
            val result = orderService.getOrderForAdmin(saved.id)

            // assert
            assertThat(result.id).isEqualTo(saved.id)
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getOrderForAdmin_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrderForAdmin(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("getAllOrders 시")
    inner class GetAllOrders {

        @Test
        @DisplayName("모든 주문이 페이지로 반환된다")
        fun getAllOrders_returnsPagedOrders() {
            // arrange
            createAndSaveOrder(1L)
            createAndSaveOrder(2L)

            // act
            val result = orderService.getAllOrders(0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(2)
        }
    }
}
