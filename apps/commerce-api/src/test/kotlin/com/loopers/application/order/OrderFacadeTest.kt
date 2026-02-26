package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.product.FailedReservation
import com.loopers.application.product.ProductService
import com.loopers.application.product.ReservedProduct
import com.loopers.application.product.StockReservationResult
import com.loopers.domain.brand.Brand
import com.loopers.domain.order.Order
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.ZonedDateTime

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

    private fun createBrand(id: Long = 1L, name: String = "나이키"): Brand {
        val brand = Brand(name = name, description = "스포츠 브랜드")
        ReflectionTestUtils.setField(brand, "id", id)
        return brand
    }

    private fun createOrder(userId: Long, items: List<OrderItemCommand>): Order {
        val order = Order(userId = userId)
        items.forEach {
            order.addItem(
                productId = it.productId,
                productName = it.productName,
                brandName = it.brandName,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
            )
        }
        ReflectionTestUtils.setField(order, "createdAt", ZonedDateTime.now())
        return order
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("재고 예약 후 주문을 생성하고 결과를 반환한다.")
        @Test
        fun reservesStockAndCreatesOrder_whenCalled() {
            // arrange
            val userId = 1L
            val product = createProduct(id = 1L, stock = 100)
            val criteria = listOf(OrderItemCriteria(productId = 1L, quantity = 2))
            val brand = createBrand(id = 1L, name = "나이키")
            val reservation = StockReservationResult(
                reservedProducts = listOf(
                    ReservedProduct(1L, "에어맥스 90", 1L, 2, BigDecimal("129000")),
                ),
                failedReservations = emptyList(),
            )
            val orderItemCommands = listOf(
                OrderItemCommand(1L, "에어맥스 90", "나이키", 2, BigDecimal("129000")),
            )
            val order = createOrder(userId, orderItemCommands)

            whenever(productService.getProductsWithLock(listOf(1L))).thenReturn(listOf(product))
            whenever(productService.reserveStock(any(), any())).thenReturn(reservation)
            whenever(brandService.getBrandsIncludingDeleted(listOf(1L))).thenReturn(listOf(brand))
            whenever(orderService.createOrder(eq(userId), any())).thenReturn(order)

            // act
            val result = orderFacade.createOrder(userId, criteria)

            // assert
            assertAll(
                { assertThat(result.order.userId).isEqualTo(userId) },
                { assertThat(result.order.items).hasSize(1) },
                { assertThat(result.excludedItems).isEmpty() },
            )
        }

        @DisplayName("일부 예약 실패 시, 성공 항목은 주문되고 실패 항목은 excludedItems로 반환된다.")
        @Test
        fun returnsExcludedItems_whenSomeReservationsFail() {
            // arrange
            val userId = 1L
            val product1 = createProduct(id = 1L, stock = 100)
            val product2 = createProduct(id = 2L, brandId = 2L, name = "울트라부스트", stock = 0)
            val criteria = listOf(
                OrderItemCriteria(productId = 1L, quantity = 2),
                OrderItemCriteria(productId = 2L, quantity = 1),
            )
            val brand = createBrand(id = 1L, name = "나이키")
            val reservation = StockReservationResult(
                reservedProducts = listOf(
                    ReservedProduct(1L, "에어맥스 90", 1L, 2, BigDecimal("129000")),
                ),
                failedReservations = listOf(
                    FailedReservation(2L, "재고가 부족합니다. 현재 재고: 0"),
                ),
            )
            val orderItemCommands = listOf(
                OrderItemCommand(1L, "에어맥스 90", "나이키", 2, BigDecimal("129000")),
            )
            val order = createOrder(userId, orderItemCommands)

            whenever(productService.getProductsWithLock(listOf(1L, 2L))).thenReturn(listOf(product1, product2))
            whenever(productService.reserveStock(any(), any())).thenReturn(reservation)
            whenever(brandService.getBrandsIncludingDeleted(listOf(1L))).thenReturn(listOf(brand))
            whenever(orderService.createOrder(eq(userId), any())).thenReturn(order)

            // act
            val result = orderFacade.createOrder(userId, criteria)

            // assert
            assertAll(
                { assertThat(result.order.items).hasSize(1) },
                { assertThat(result.excludedItems).hasSize(1) },
                { assertThat(result.excludedItems[0].productId).isEqualTo(2L) },
            )
        }

        @DisplayName("전체 예약 실패 시, BAD_REQUEST 예외가 전파된다.")
        @Test
        fun throwsBadRequest_whenAllReservationsFail() {
            // arrange
            val userId = 1L
            val product = createProduct(id = 1L, stock = 0)
            val criteria = listOf(OrderItemCriteria(productId = 1L, quantity = 2))
            val reservation = StockReservationResult(
                reservedProducts = emptyList(),
                failedReservations = listOf(
                    FailedReservation(1L, "재고가 부족합니다. 현재 재고: 0"),
                ),
            )

            whenever(productService.getProductsWithLock(listOf(1L))).thenReturn(listOf(product))
            whenever(productService.reserveStock(any(), any())).thenReturn(reservation)
            whenever(orderService.createOrder(eq(userId), any()))
                .thenThrow(CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다."))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(userId, criteria)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
