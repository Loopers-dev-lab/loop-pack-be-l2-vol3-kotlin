package com.loopers.application.order

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderFacade")
class OrderFacadeTest {

    private val orderService: OrderService = mockk()
    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val orderFacade = OrderFacade(orderService, productService, brandService)

    companion object {
        private const val USER_ID = 1L
        private const val BRAND_ID = 10L
        private const val BRAND_NAME = "루프팩"
        private const val PRODUCT_NAME_1 = "감성 티셔츠"
        private const val PRODUCT_NAME_2 = "캔버스백"
    }

    private fun createProduct(
        id: Long = 1L,
        name: String = PRODUCT_NAME_1,
        price: Long = 25000L,
        brandId: Long = BRAND_ID,
        stockQuantity: Int = 100,
    ): ProductModel {
        val product = ProductModel(
            name = name,
            price = price,
            brandId = brandId,
            stockQuantity = stockQuantity,
        )
        return spyk(product) {
            every { this@spyk.id } returns id
        }
    }

    private fun createBrand(
        id: Long = BRAND_ID,
        name: String = BRAND_NAME,
    ): BrandModel {
        val brand = BrandModel(name = name)
        return spyk(brand) {
            every { this@spyk.id } returns id
        }
    }

    @DisplayName("createOrder")
    @Nested
    inner class CreateOrder {
        @DisplayName("정상적인 단일 상품 주문이 생성된다")
        @Test
        fun createsOrder_whenSingleProductWithSufficientStock() {
            // arrange
            val product = createProduct(id = 1L, stockQuantity = 10)
            val brand = createBrand()
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 3))

            every { productService.findAllByIds(listOf(1L)) } returns listOf(product)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            every {
                orderService.createOrder(
                    userId = USER_ID,
                    orderItems = any(),
                    brandNameResolver = any(),
                )
            } answers {
                val orderItems = secondArg<List<Pair<ProductModel, Int>>>()
                val resolver = thirdArg<(Long) -> String>()
                val order = OrderModel(userId = USER_ID)
                orderItems.forEach { (prod, qty) ->
                    prod.decreaseStock(qty)
                    val item = OrderItemModel(
                        order = order,
                        productId = prod.id,
                        productName = prod.name,
                        brandName = resolver(prod.brandId),
                        price = prod.price,
                        quantity = qty,
                    )
                    order.addItem(item)
                }
                order
            }

            // act
            val result = orderFacade.createOrder(USER_ID, items)

            // assert
            assertThat(result.userId).isEqualTo(USER_ID)
            assertThat(result.orderStatus).isEqualTo(OrderStatus.ORDERED)
            assertThat(result.orderItems).hasSize(1)
            assertThat(result.totalAmount).isEqualTo(25000L * 3)
            verify(exactly = 1) { productService.findAllByIds(listOf(1L)) }
            verify(exactly = 1) { brandService.findAllByIds(listOf(BRAND_ID)) }
            verify(exactly = 1) { orderService.createOrder(USER_ID, any(), any()) }
        }

        @DisplayName("다중 상품 주문이 정상적으로 생성되고 총 금액이 정확하다")
        @Test
        fun createsOrder_whenMultipleProductsWithSufficientStock() {
            // arrange
            val product1 = createProduct(id = 1L, name = PRODUCT_NAME_1, price = 25000L, stockQuantity = 10)
            val product2 = createProduct(id = 2L, name = PRODUCT_NAME_2, price = 5000L, stockQuantity = 20)
            val brand = createBrand()
            val items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 2L, quantity = 1),
            )

            every { productService.findAllByIds(listOf(1L, 2L)) } returns listOf(product1, product2)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            every {
                orderService.createOrder(
                    userId = USER_ID,
                    orderItems = any(),
                    brandNameResolver = any(),
                )
            } answers {
                val orderItems = secondArg<List<Pair<ProductModel, Int>>>()
                val resolver = thirdArg<(Long) -> String>()
                val order = OrderModel(userId = USER_ID)
                orderItems.forEach { (prod, qty) ->
                    prod.decreaseStock(qty)
                    val item = OrderItemModel(
                        order = order,
                        productId = prod.id,
                        productName = prod.name,
                        brandName = resolver(prod.brandId),
                        price = prod.price,
                        quantity = qty,
                    )
                    order.addItem(item)
                }
                order
            }

            // act
            val result = orderFacade.createOrder(USER_ID, items)

            // assert
            assertThat(result.orderItems).hasSize(2)
            assertThat(result.totalAmount).isEqualTo(25000L * 2 + 5000L * 1)
        }

        @DisplayName("중복 상품 ID가 포함되면 BAD_REQUEST 예외가 발생한다")
        @Test
        fun throwsBadRequest_whenDuplicateProductIds() {
            // arrange
            val items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 1L, quantity = 3),
            )

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            verify(exactly = 0) { productService.findAllByIds(any()) }
            verify(exactly = 0) { orderService.createOrder(any(), any(), any()) }
        }

        @DisplayName("존재하지 않는 상품이 포함되면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // arrange
            val product = createProduct(id = 1L)
            val items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 999L, quantity = 1),
            )

            every { productService.findAllByIds(listOf(1L, 999L)) } returns listOf(product)

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)

            verify(exactly = 1) { productService.findAllByIds(listOf(1L, 999L)) }
            verify(exactly = 0) { orderService.createOrder(any(), any(), any()) }
        }

        @DisplayName("재고 부족 시 CoreException이 발생한다")
        @Test
        fun throwsBadRequest_whenInsufficientStock() {
            // arrange
            val product = createProduct(id = 1L, stockQuantity = 3)
            val brand = createBrand()
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 5))

            every { productService.findAllByIds(listOf(1L)) } returns listOf(product)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            every {
                orderService.createOrder(
                    userId = USER_ID,
                    orderItems = any(),
                    brandNameResolver = any(),
                )
            } throws CoreException(
                ErrorType.BAD_REQUEST,
                "상품의 재고가 부족합니다.",
            )

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("재고가 부족합니다")
        }
    }
}
