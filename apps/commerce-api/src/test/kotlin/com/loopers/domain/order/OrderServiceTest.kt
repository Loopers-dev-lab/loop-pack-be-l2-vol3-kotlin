package com.loopers.domain.order

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

class OrderServiceTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        orderService = OrderService(orderRepository, orderItemRepository)
    }

    private fun createProductInfo(id: Long, name: String, price: BigDecimal): OrderProductInfo {
        return OrderProductInfo(id = id, name = name, price = price)
    }

    private fun createAndSaveOrder(userId: Long): OrderDetail {
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
            val detail = orderService.createOrder(1L, listOf(productInfo), command)

            // assert
            assertThat(detail.order.id).isNotEqualTo(0L)
            assertThat(detail.order.refUserId).isEqualTo(1L)
            assertThat(detail.order.totalPrice).isEqualByComparingTo(BigDecimal("258000"))
            assertThat(detail.items).hasSize(1)
            assertThat(detail.items[0].refProductId).isEqualTo(1L)
            assertThat(detail.items[0].productName).isEqualTo("에어맥스 90")
            assertThat(detail.items[0].productPrice).isEqualByComparingTo(BigDecimal("129000"))
            assertThat(detail.items[0].quantity).isEqualTo(2)
        }

        @Test
        @DisplayName("여러 상품이 포함된 주문의 총액이 올바르게 계산된다")
        fun createOrder_multipleItems_calculatesTotalPrice() {
            // arrange
            val product1 = createProductInfo(1L, "상품1", BigDecimal("10000"))
            val product2 = createProductInfo(2L, "상품2", BigDecimal("20000"))
            val command = OrderCommand.CreateOrder(
                items = listOf(
                    OrderCommand.CreateOrderItem(productId = 1L, quantity = 3),
                    OrderCommand.CreateOrderItem(productId = 2L, quantity = 2),
                ),
            )

            // act
            val detail = orderService.createOrder(1L, listOf(product1, product2), command)

            // assert
            assertThat(detail.items).hasSize(2)
            assertThat(detail.order.totalPrice).isEqualByComparingTo(BigDecimal("70000"))
        }

        @Test
        @DisplayName("존재하지 않는 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun createOrder_missingProduct_throwsException() {
            // arrange
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = 999L, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderService.createOrder(1L, emptyList(), command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("상품을 찾을 수 없습니다")
        }
    }

    @Nested
    @DisplayName("getOrder 시")
    inner class GetOrder {

        @Test
        @DisplayName("주문 소유자가 조회하면 OrderDetail이 반환된다")
        fun getOrder_owner_returnsOrderDetail() {
            // arrange
            val saved = createAndSaveOrder(1L)

            // act
            val result = orderService.getOrder(1L, saved.order.id)

            // assert
            assertThat(result.order.id).isEqualTo(saved.order.id)
            assertThat(result.order.refUserId).isEqualTo(1L)
            assertThat(result.items).hasSize(1)
        }

        @Test
        @DisplayName("다른 사용자가 조회하면 NOT_FOUND 예외가 발생한다")
        fun getOrder_otherUser_throwsNotFound() {
            // arrange
            val saved = createAndSaveOrder(1L)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(2L, saved.order.id)
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
        @DisplayName("주문이 존재하면 소유자와 관계없이 OrderDetail이 반환된다")
        fun getOrderForAdmin_existingOrder_returnsOrderDetail() {
            // arrange
            val saved = createAndSaveOrder(1L)

            // act
            val result = orderService.getOrderForAdmin(saved.order.id)

            // assert
            assertThat(result.order.id).isEqualTo(saved.order.id)
            assertThat(result.items).hasSize(1)
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
    @DisplayName("getOrdersByUserId 시")
    inner class GetOrdersByUserId {

        @Test
        @DisplayName("해당 사용자의 주문만 반환된다")
        fun getOrdersByUserId_returnsOnlyUserOrders() {
            // arrange
            createAndSaveOrder(1L)
            createAndSaveOrder(1L)
            createAndSaveOrder(2L)

            val from = ZonedDateTime.now().minusDays(1)
            val to = ZonedDateTime.now().plusDays(1)

            // act
            val result = orderService.getOrdersByUserId(1L, from, to, 0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content).allMatch { it.order.refUserId == 1L }
        }

        @Test
        @DisplayName("기간 범위 밖의 주문은 제외된다")
        fun getOrdersByUserId_excludesOutOfRange() {
            // arrange
            createAndSaveOrder(1L)

            val from = ZonedDateTime.now().plusDays(1)
            val to = ZonedDateTime.now().plusDays(2)

            // act
            val result = orderService.getOrdersByUserId(1L, from, to, 0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("주문이 없으면 빈 페이지가 반환된다")
        fun getOrdersByUserId_noOrders_returnsEmpty() {
            // arrange
            val from = ZonedDateTime.now().minusDays(1)
            val to = ZonedDateTime.now().plusDays(1)

            // act
            val result = orderService.getOrdersByUserId(1L, from, to, 0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        fun getOrdersByUserId_pagination_works() {
            // arrange
            repeat(5) { createAndSaveOrder(1L) }

            val from = ZonedDateTime.now().minusDays(1)
            val to = ZonedDateTime.now().plusDays(1)

            // act
            val page0 = orderService.getOrdersByUserId(1L, from, to, 0, 2)
            val page1 = orderService.getOrdersByUserId(1L, from, to, 1, 2)

            // assert
            assertThat(page0.totalElements).isEqualTo(5)
            assertThat(page0.content).hasSize(2)
            assertThat(page1.content).hasSize(2)
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
