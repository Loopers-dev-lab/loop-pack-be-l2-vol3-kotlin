package com.loopers.application.order

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.product.ProductModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderServiceTest {
    private lateinit var orderService: OrderService
    private lateinit var fakeRepository: FakeOrderRepository

    @BeforeEach
    fun setUp() {
        fakeRepository = FakeOrderRepository()
        orderService = OrderService(fakeRepository)
    }

    private fun createProduct(id: Long = 1L, brandId: Long = 10L): ProductModel {
        return ProductModel(
            id = id,
            brandId = brandId,
            name = "감성 티셔츠",
            description = "좋은 상품",
            price = 39000,
            stockQuantity = 100,
            imageUrl = "https://example.com/product.jpg",
        )
    }

    private fun defaultProductMap(): Map<Long, ProductModel> {
        val product = createProduct()
        return mapOf(product.id to product)
    }

    private fun defaultBrandNames(): Map<Long, String> = mapOf(10L to "루퍼스")

    private fun defaultItems(): List<OrderCommand.CreateOrderItem> =
        listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = 2))

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrder_whenValidInfoProvided() {
            // act
            val order = orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), defaultItems())

            // assert
            assertAll(
                { assertThat(order.memberId).isEqualTo(1L) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(order.items).hasSize(1) },
                { assertThat(order.items[0].productName).isEqualTo("감성 티셔츠") },
                { assertThat(order.items[0].brandName).isEqualTo("루퍼스") },
                { assertThat(order.items[0].amount).isEqualTo(78000L) },
                { assertThat(order.getTotalAmount()).isEqualTo(78000L) },
            )
        }

        @DisplayName("주문 항목이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenItemsEmpty() {
            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), emptyList())
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문 수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZero() {
            // arrange
            val items = listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = 0))

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), items)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문 수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsNegative() {
            // arrange
            val items = listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = -1))

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), items)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 상품이 포함되면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotInMap() {
            // arrange
            val items = listOf(OrderCommand.CreateOrderItem(productId = 999L, quantity = 1))

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), items)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("주문을 조회할 때,")
    @Nested
    inner class GetOrder {
        @DisplayName("본인 주문을 조회하면, 주문을 반환한다.")
        @Test
        fun returnsOrder_whenOwner() {
            // arrange
            val created = orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), defaultItems())

            // act
            val result = orderService.getOrder(created.id, 1L)

            // assert
            assertThat(result.id).isEqualTo(created.id)
        }

        @DisplayName("다른 사용자의 주문을 조회하면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsForbidden_whenNotOwner() {
            // arrange
            val created = orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), defaultItems())

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.getOrder(created.id, 999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @DisplayName("존재하지 않는 주문을 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderDoesNotExist() {
            // act & assert
            val result = assertThrows<CoreException> {
                orderService.getOrder(999L, 1L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("어드민 주문을 조회할 때,")
    @Nested
    inner class GetOrderById {
        @DisplayName("존재하는 주문을 조회하면, validateOwner 없이 주문을 반환한다.")
        @Test
        fun returnsOrder_withoutOwnerValidation() {
            // arrange
            val created = orderService.createOrder(1L, defaultProductMap(), defaultBrandNames(), defaultItems())

            // act
            val result = orderService.getOrderById(created.id)

            // assert
            assertThat(result.id).isEqualTo(created.id)
        }

        @DisplayName("존재하지 않는 주문을 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderDoesNotExist() {
            // act & assert
            val result = assertThrows<CoreException> {
                orderService.getOrderById(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
