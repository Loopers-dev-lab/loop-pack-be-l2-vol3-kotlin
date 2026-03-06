package com.loopers.domain.order

import com.loopers.domain.product.ProductModel
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderService")
class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()
    private val orderService = OrderService(orderRepository)

    companion object {
        private const val USER_ID = 1L
        private const val BRAND_NAME = "루프팩"
    }

    private fun createProduct(
        name: String = "감성 티셔츠",
        price: Long = 25000L,
        brandId: Long = 1L,
        stockQuantity: Int = 100,
    ): ProductModel = ProductModel(
        name = name,
        price = price,
        brandId = brandId,
        stockQuantity = stockQuantity,
    )

    private val brandNameResolver: (Long) -> String = { BRAND_NAME }

    @DisplayName("createOrder")
    @Nested
    inner class CreateOrder {
        @DisplayName("단일 상품으로 정상 주문을 생성하면 재고가 차감되고 주문이 저장된다")
        @Test
        fun createsOrder_whenSingleProductWithSufficientStock() {
            // arrange
            val product = createProduct(stockQuantity = 10)
            val orderItems = listOf(product to 3)
            every { orderRepository.save(any()) } answers { firstArg() }

            // act
            val order = orderService.createOrder(
                userId = USER_ID,
                orderItems = orderItems,
                brandNameResolver = brandNameResolver,
            )

            // assert
            assertThat(order.userId).isEqualTo(USER_ID)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.ORDERED)
            assertThat(order.orderItems).hasSize(1)
            assertThat(order.totalAmount).isEqualTo(25000L * 3)
            assertThat(product.stockQuantity).isEqualTo(7)
            verify(exactly = 1) { orderRepository.save(any()) }
        }

        @DisplayName("다중 상품으로 정상 주문을 생성하면 각 상품의 재고가 차감되고 총 금액이 정확하다")
        @Test
        fun createsOrder_whenMultipleProductsWithSufficientStock() {
            // arrange
            val product1 = createProduct(
                name = "감성 티셔츠",
                price = 25000L,
                brandId = 1L,
                stockQuantity = 10,
            )
            val product2 = createProduct(
                name = "캔버스백",
                price = 5000L,
                brandId = 1L,
                stockQuantity = 20,
            )
            val orderItems = listOf(product1 to 2, product2 to 1)
            every { orderRepository.save(any()) } answers { firstArg() }

            // act
            val order = orderService.createOrder(
                userId = USER_ID,
                orderItems = orderItems,
                brandNameResolver = brandNameResolver,
            )

            // assert
            assertThat(order.orderItems).hasSize(2)
            assertThat(order.totalAmount).isEqualTo(25000L * 2 + 5000L * 1)
            assertThat(product1.stockQuantity).isEqualTo(8)
            assertThat(product2.stockQuantity).isEqualTo(19)
            verify(exactly = 1) { orderRepository.save(any()) }
        }

        @DisplayName("재고 부족 시 CoreException이 발생하고 주문이 저장되지 않는다")
        @Test
        fun throwsException_whenInsufficientStock() {
            // arrange
            val product = createProduct(stockQuantity = 3)
            val orderItems = listOf(product to 5)

            // act & assert
            assertThatThrownBy {
                orderService.createOrder(
                    userId = USER_ID,
                    orderItems = orderItems,
                    brandNameResolver = brandNameResolver,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("재고가 부족합니다")

            verify(exactly = 0) { orderRepository.save(any()) }
        }

        @DisplayName("스냅샷에 주문 시점의 상품명, 가격, 브랜드명이 저장된다")
        @Test
        fun savesSnapshot_withProductInfoAtOrderTime() {
            // arrange
            val productName = "한정판 후드티"
            val brandName = "프리미엄브랜드"
            val price = 89000L
            val product = createProduct(
                name = productName,
                price = price,
                brandId = 2L,
                stockQuantity = 50,
            )
            val orderItems = listOf(product to 1)
            val resolver: (Long) -> String = { brandName }
            every { orderRepository.save(any()) } answers { firstArg() }

            // act
            val order = orderService.createOrder(
                userId = USER_ID,
                orderItems = orderItems,
                brandNameResolver = resolver,
            )

            // assert
            val savedItem = order.orderItems[0]
            assertThat(savedItem.productName).isEqualTo(productName)
            assertThat(savedItem.brandName).isEqualTo(brandName)
            assertThat(savedItem.price).isEqualTo(price)
            assertThat(savedItem.subTotal).isEqualTo(price)
        }

        @DisplayName("다중 상품 주문 중 두 번째 상품의 재고가 부족하면 전체 주문이 실패한다")
        @Test
        fun failsEntireOrder_whenSecondProductHasInsufficientStock() {
            // arrange
            val product1 = createProduct(
                name = "감성 티셔츠",
                price = 25000L,
                stockQuantity = 10,
            )
            val product2 = createProduct(
                name = "캔버스백",
                price = 5000L,
                stockQuantity = 2,
            )
            val orderItems = listOf(product1 to 3, product2 to 5)

            // act & assert
            assertThatThrownBy {
                orderService.createOrder(
                    userId = USER_ID,
                    orderItems = orderItems,
                    brandNameResolver = brandNameResolver,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            verify(exactly = 0) { orderRepository.save(any()) }
        }
    }
}
