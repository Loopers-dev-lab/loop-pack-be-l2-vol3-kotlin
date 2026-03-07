package com.loopers.domain.order

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class OrderItemTest {

    @Test
    fun `OrderItem 생성 - orderId 필수 파라미터로 설정`() {
        // Arrange
        val orderId = 100L
        val productId = 1L
        val productName = "상품명"
        val quantity = 2
        val price = BigDecimal("10000")

        // Act
        val item = OrderItem.create(
            orderId = orderId,
            productId = productId,
            productName = productName,
            quantity = quantity,
            price = price,
        )

        // Assert
        assertEquals(orderId, item.orderId)
        assertEquals(productId, item.productId)
        assertEquals(productName, item.productName)
        assertEquals(quantity, item.quantity)
        assertEquals(price, item.price)
    }

    @Test
    fun `OrderItem 생성 - 수량이 0 이하면 실패`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            OrderItem.create(
                orderId = 100L,
                productId = 1L,
                productName = "상품명",
                quantity = 0,
                price = BigDecimal("10000"),
            )
        }
    }

    @Test
    fun `OrderItem 생성 - 가격이 0 이하면 실패`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            OrderItem.create(
                orderId = 100L,
                productId = 1L,
                productName = "상품명",
                quantity = 1,
                price = BigDecimal.ZERO,
            )
        }
    }
}
