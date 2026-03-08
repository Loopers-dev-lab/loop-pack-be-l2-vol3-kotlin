package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class OrderTest {

    private fun createBrand(name: String = "Test Brand", description: String = "Test Description"): Brand {
        return Brand.create(name = name, description = description)
    }

    private fun createProduct(
        brand: Brand,
        name: String = "Test Product",
        price: BigDecimal = BigDecimal("10000"),
        status: ProductStatus = ProductStatus.ACTIVE,
    ): Product {
        return Product.create(
            brand = brand,
            name = name,
            price = price,
            status = status,
        )
    }

    @Test
    fun `Order createWithItems - 여러 OrderItem을 함께 생성`() {
        // Arrange
        val userId = 1L
        val brand = createBrand()
        val product1 = createProduct(brand, name = "상품1", price = BigDecimal("10000"))
        val product2 = createProduct(brand, name = "상품2", price = BigDecimal("20000"))

        val itemSpecs = listOf(
            OrderItemSpec(product1, 2, BigDecimal("10000")),
            OrderItemSpec(product2, 1, BigDecimal("20000")),
        )

        // Act
        val order = Order.createWithItems(userId, null, itemSpecs)

        // Assert
        assertEquals(userId, order.userId)
        assertEquals(2, order.orderItems.size)
        // Product가 저장되지 않았으므로 productId는 0L이 됨
        assertEquals(0L, order.orderItems[0].productId)
        assertEquals(0L, order.orderItems[1].productId)
        // 단, productName은 설정되어야 함
        assertEquals("상품1", order.orderItems[0].productName)
        assertEquals("상품2", order.orderItems[1].productName)
        assertEquals(BigDecimal("40000"), order.getTotalPrice())
    }

    @Test
    fun `Order createWithItems - 빈 items 리스트면 실패`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            Order.createWithItems(1L, null, emptyList())
        }
    }
}
