package com.loopers.application.order

import com.loopers.domain.catalog.brand.Brand
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.Product
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.catalog.product.ProductStatus
import com.loopers.domain.catalog.product.ProductStock
import com.loopers.domain.catalog.product.ProductStockService
import com.loopers.domain.coupon.CouponTemplateService
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderFacadeUnitTest {

    private val mockOrderService = mockk<OrderService>()
    private val mockProductService = mockk<ProductService>()
    private val mockBrandService = mockk<BrandService>()
    private val mockProductStockService = mockk<ProductStockService>()
    private val mockUserCouponService = mockk<UserCouponService>()
    private val mockCouponTemplateService = mockk<CouponTemplateService>()

    private val orderFacade = OrderFacade(
        mockOrderService, mockProductService, mockBrandService,
        mockProductStockService, mockUserCouponService, mockCouponTemplateService
    )

    // ─── placeOrder ───

    @Test
    fun `placeOrder() should fetch products, validate stock, decrement stock, and create order`() {
        // Arrange
        val product = createProduct(id = 1L, brandId = 1L)
        val stock = createStock(productId = 1L, quantity = 10)
        val decrementedStock = createStock(productId = 1L, quantity = 8)
        val brand = createBrand(id = 1L, name = "Nike")
        val orderItem = createOrderItem(productId = 1L, brandId = 1L)
        val order = createOrder(userId = 1L, items = listOf(orderItem))

        every { mockProductService.getById(1L) } returns product
        every { mockProductStockService.getByProductId(1L) } returns stock
        every { mockProductStockService.decrementStock(1L, 2) } returns decrementedStock
        every { mockBrandService.getById(1L) } returns brand
        every { mockOrderService.createOrder(any(), any(), any(), any()) } returns order

        val cmd = PlaceOrderCommand(items = listOf(OrderItemCommand(productId = 1L, quantity = 2)))

        // Act
        val result = orderFacade.placeOrder(userId = 1L, cmd = cmd)

        // Assert
        assertThat(result).isNotNull
        verify { mockProductService.getById(1L) }
        verify { mockProductStockService.decrementStock(1L, 2) }
        verify { mockOrderService.createOrder(1L, any(), any(), any()) }
    }

    @Test
    fun `placeOrder() throws BAD_REQUEST when one product is out of stock`() {
        // Arrange
        val product1 = createProduct(id = 1L)
        val product2 = createProduct(id = 2L)
        val stock1 = createStock(productId = 1L, quantity = 10)
        val stock2 = createStock(productId = 2L, quantity = 1) // only 1 in stock

        every { mockProductService.getById(1L) } returns product1
        every { mockProductService.getById(2L) } returns product2
        every { mockProductStockService.getByProductId(1L) } returns stock1
        every { mockProductStockService.getByProductId(2L) } returns stock2

        val cmd = PlaceOrderCommand(
            items = listOf(
                OrderItemCommand(productId = 1L, quantity = 2),
                OrderItemCommand(productId = 2L, quantity = 5), // exceeds stock
            )
        )

        // Act & Assert
        assertThrows<CoreException> {
            orderFacade.placeOrder(userId = 1L, cmd = cmd)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        // stock should NOT be decremented for any product
        verify(exactly = 0) { mockProductStockService.decrementStock(any(), any()) }
        verify(exactly = 0) { mockOrderService.createOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `placeOrder() throws NOT_FOUND when product does not exist`() {
        // Arrange
        every { mockProductService.getById(99L) } throws CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

        val cmd = PlaceOrderCommand(items = listOf(OrderItemCommand(productId = 99L, quantity = 1)))

        // Act & Assert
        assertThrows<CoreException> {
            orderFacade.placeOrder(userId = 1L, cmd = cmd)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        verify(exactly = 0) { mockProductStockService.decrementStock(any(), any()) }
        verify(exactly = 0) { mockOrderService.createOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `placeOrder() throws BAD_REQUEST when product is not orderable`() {
        // Arrange
        val hiddenProduct = createProduct(id = 1L, status = ProductStatus.HIDDEN)
        val stock = createStock(productId = 1L, quantity = 10)
        every { mockProductService.getById(1L) } returns hiddenProduct
        every { mockProductStockService.getByProductId(1L) } returns stock

        val cmd = PlaceOrderCommand(items = listOf(OrderItemCommand(productId = 1L, quantity = 1)))

        // Act & Assert
        assertThrows<CoreException> {
            orderFacade.placeOrder(userId = 1L, cmd = cmd)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        verify(exactly = 0) { mockProductStockService.decrementStock(any(), any()) }
        verify(exactly = 0) { mockOrderService.createOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `placeOrder() throws BAD_REQUEST when items list is empty`() {
        // Act & Assert
        assertThrows<CoreException> {
            orderFacade.placeOrder(userId = 1L, cmd = PlaceOrderCommand(items = emptyList()))
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        verify(exactly = 0) { mockProductService.getById(any()) }
        verify(exactly = 0) { mockOrderService.createOrder(any(), any(), any(), any()) }
    }

    private fun createProduct(
        id: Long = 0L,
        brandId: Long = 1L,
        name: String = "Test Product",
        status: ProductStatus = ProductStatus.ACTIVE,
    ): Product = Product(id = id, brandId = brandId, name = name, description = "desc", price = 10000, status = status)

    private fun createStock(
        productId: Long = 1L,
        quantity: Int = 10,
        id: Long = 0L,
    ): ProductStock = ProductStock(productId = productId, quantity = quantity, id = id)

    private fun createBrand(id: Long = 0L, name: String = "TestBrand"): Brand =
        Brand(id = id, name = name, description = "desc")

    private fun createOrderItem(
        orderId: Long = 0L,
        productId: Long = 1L,
        brandId: Long = 1L,
        price: Int = 10000,
        quantity: Int = 2,
    ): OrderItem = OrderItem(
        orderId = orderId,
        productId = productId,
        productName = "Test Product",
        brandId = brandId,
        brandName = "Test Brand",
        price = price,
        quantity = quantity,
    )

    private fun createOrder(
        id: Long = 0L,
        userId: Long = 1L,
        items: List<OrderItem>,
    ): Order = Order(
        id = id,
        userId = userId,
        items = items,
        originalTotalPrice = items.sumOf { it.subtotal() },
    )
}
