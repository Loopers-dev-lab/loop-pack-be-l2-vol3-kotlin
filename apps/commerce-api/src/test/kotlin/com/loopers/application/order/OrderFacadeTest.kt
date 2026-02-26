package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductService
import com.loopers.domain.brand.Brand
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class OrderFacadeTest {

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var brandService: BrandService

    @InjectMocks
    private lateinit var orderFacade: OrderFacade

    private fun createProduct(
        id: Long = 1L,
        brandId: Long = 1L,
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
    ): Product {
        val product = Product(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            description = null,
            imageUrl = null,
        )
        ReflectionTestUtils.setField(product, "id", id)
        return product
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("모든 상품의 재고가 충분하면, 주문이 생성된다.")
        @Test
        fun createsOrder_whenAllStockSufficient() {
            // arrange
            val userId = 1L
            val product = createProduct(id = 1L, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100)
            val commands = listOf(OrderItemCommand(productId = 1L, quantity = 2))
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            whenever(productService.getProductsWithLock(listOf(1L))).thenReturn(listOf(product))
            whenever(brandService.getBrandIncludingDeleted(1L)).thenReturn(brand)
            whenever(orderService.createOrder(any())).thenAnswer { it.arguments[0] }

            // act
            val result = orderFacade.createOrder(userId, commands)

            // assert
            assertAll(
                { assertThat(result.order.userId).isEqualTo(userId) },
                { assertThat(result.order.orderItems).hasSize(1) },
                { assertThat(result.order.totalAmount).isEqualByComparingTo(BigDecimal("258000")) },
                { assertThat(result.excludedItems).isEmpty() },
                { assertThat(product.stock).isEqualTo(98) },
            )
        }

        @DisplayName("일부 상품의 재고가 부족하면, 부분 주문이 생성된다.")
        @Test
        fun createsPartialOrder_whenSomeStockInsufficient() {
            // arrange
            val userId = 1L
            val product1 = createProduct(id = 1L, brandId = 1L, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100)
            val product2 = createProduct(id = 2L, brandId = 2L, name = "울트라부스트", price = BigDecimal("199000"), stock = 0)
            val commands = listOf(
                OrderItemCommand(productId = 1L, quantity = 2),
                OrderItemCommand(productId = 2L, quantity = 1),
            )
            val brand1 = Brand(name = "나이키", description = "스포츠 브랜드")

            whenever(productService.getProductsWithLock(listOf(1L, 2L))).thenReturn(listOf(product1, product2))
            whenever(brandService.getBrandIncludingDeleted(1L)).thenReturn(brand1)
            whenever(orderService.createOrder(any())).thenAnswer { it.arguments[0] }

            // act
            val result = orderFacade.createOrder(userId, commands)

            // assert
            assertAll(
                { assertThat(result.order.orderItems).hasSize(1) },
                { assertThat(result.order.totalAmount).isEqualByComparingTo(BigDecimal("258000")) },
                { assertThat(result.excludedItems).hasSize(1) },
                { assertThat(result.excludedItems[0].productId).isEqualTo(2L) },
            )
        }

        @DisplayName("모든 상품의 재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAllStockInsufficient() {
            // arrange
            val userId = 1L
            val product = createProduct(id = 1L, name = "에어맥스 90", stock = 0)
            val commands = listOf(OrderItemCommand(productId = 1L, quantity = 2))

            whenever(productService.getProductsWithLock(listOf(1L))).thenReturn(listOf(product))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(userId, commands)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 상품은 제외된다.")
        @Test
        fun excludesNonExistentProducts() {
            // arrange
            val userId = 1L
            val product = createProduct(id = 1L, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100)
            val commands = listOf(
                OrderItemCommand(productId = 1L, quantity = 2),
                OrderItemCommand(productId = 999L, quantity = 1),
            )
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            whenever(productService.getProductsWithLock(listOf(1L, 999L))).thenReturn(listOf(product))
            whenever(brandService.getBrandIncludingDeleted(1L)).thenReturn(brand)
            whenever(orderService.createOrder(any())).thenAnswer { it.arguments[0] }

            // act
            val result = orderFacade.createOrder(userId, commands)

            // assert
            assertAll(
                { assertThat(result.order.orderItems).hasSize(1) },
                { assertThat(result.excludedItems).hasSize(1) },
                { assertThat(result.excludedItems[0].productId).isEqualTo(999L) },
                { assertThat(result.excludedItems[0].reason).contains("존재하지 않") },
            )
        }
    }
}
