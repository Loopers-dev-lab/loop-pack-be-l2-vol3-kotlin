package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

@DisplayName("OrderItem")
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

    @Test
    @DisplayName("할인액이 항목 금액과 같을 때 정상 적용")
    fun `applyDiscountAmount - discount equals item amount`() {
        // Arrange
        val item = OrderItem.create(
            orderId = 100L,
            productId = 1L,
            productName = "상품명",
            quantity = 2,
            price = BigDecimal("10000"),
        )
        val itemAmount = item.getItemAmount() // 20000
        val discount = itemAmount

        // Act
        item.applyDiscountAmount(discount)

        // Assert
        assertEquals(discount, item.discountAmount)
        assertEquals(BigDecimal.ZERO, item.getSubtotal())
    }

    @Test
    @DisplayName("할인액이 항목 금액을 초과하면 예외 발생")
    fun `applyDiscountAmount - discount exceeds item amount`() {
        // Arrange
        val item = OrderItem.create(
            orderId = 100L,
            productId = 1L,
            productName = "상품명",
            quantity = 2,
            price = BigDecimal("10000"),
        )
        val itemAmount = item.getItemAmount() // 20000
        val discount = itemAmount.add(BigDecimal("1"))

        // Act & Assert
        val exception = assertThrows<CoreException> {
            item.applyDiscountAmount(discount)
        }
        assertEquals(ErrorType.BAD_REQUEST, exception.errorType)
    }

    @Test
    @DisplayName("같은 OrderItem에 할인을 두 번 적용하면 예외 발생")
    fun `applyDiscountAmount - applying discount twice`() {
        // Arrange
        val item = OrderItem.create(
            orderId = 100L,
            productId = 1L,
            productName = "상품명",
            quantity = 2,
            price = BigDecimal("10000"),
        )

        // Act - 첫 번째 할인 적용
        item.applyDiscountAmount(BigDecimal("5000"))

        // Assert - 두 번째 할인 적용 시 예외 발생
        val exception = assertThrows<CoreException> {
            item.applyDiscountAmount(BigDecimal("3000"))
        }
        assertEquals(ErrorType.BAD_REQUEST, exception.errorType)
    }
}
