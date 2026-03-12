package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderUnitTest {

    // ─── OrderItem.subtotal ───

    @Test
    fun `subtotal() should return price multiplied by quantity`() {
        // Arrange
        val item = createOrderItem(price = 10000, quantity = 3)

        // Act & Assert
        assertThat(item.subtotal()).isEqualTo(30000)
    }

    @Test
    fun `subtotal() should return 0 when price is 0`() {
        // Arrange
        val item = createOrderItem(price = 0, quantity = 5)

        // Assert
        assertThat(item.subtotal()).isEqualTo(0)
    }

    // ─── Order.create ───

    @Test
    fun `Order create() should compute correct totalPrice`() {
        // Arrange
        val items = listOf(
            createOrderItem(price = 10000, quantity = 2),
            createOrderItem(price = 5000, quantity = 3),
        )

        // Act
        val order = Order.create(userId = 1L, items = items)

        // Assert
        assertThat(order.totalPrice).isEqualTo(35000) // 20000 + 15000
        assertThat(order.items).hasSize(2)
        assertThat(order.userId).isEqualTo(1L)
    }

    @Test
    fun `Order create() throws CoreException(BAD_REQUEST) when items is empty`() {
        // Act & Assert
        assertThrows<CoreException> {
            Order.create(userId = 1L, items = emptyList())
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    // ─── OrderItem init ───

    @Test
    fun `OrderItem init throws CoreException(BAD_REQUEST) when quantity is 0`() {
        assertThrows<CoreException> {
            createOrderItem(quantity = 0)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Test
    fun `OrderItem init throws CoreException(BAD_REQUEST) when productName is blank`() {
        assertThrows<CoreException> {
            createOrderItem(productName = "  ")
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Test
    fun `OrderItem init throws CoreException(BAD_REQUEST) when brandName is blank`() {
        assertThrows<CoreException> {
            createOrderItem(brandName = "  ")
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    private fun createOrderItem(
        orderId: Long = 0L,
        productId: Long = 1L,
        productName: String = "Test Product",
        brandId: Long = 1L,
        brandName: String = "Test Brand",
        price: Int = 10000,
        quantity: Int = 1,
    ): OrderItem = OrderItem(
        orderId = orderId,
        productId = productId,
        productName = productName,
        brandId = brandId,
        brandName = brandName,
        price = price,
        quantity = quantity,
    )
}
