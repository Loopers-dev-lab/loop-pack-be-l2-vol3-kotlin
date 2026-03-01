package com.loopers.domain.order

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("DiscountDistributer")
class DiscountDistributerTest {

    @DisplayName("할인액을 가격 비율에 따라 분배한다")
    @Test
    fun distributeDiscount_distributesByPrice() {
        // Arrange
        val item1 = OrderItem.create(
            orderId = 1L,
            productId = 1L,
            quantity = 1,
            price = BigDecimal("10000"),
            productName = "Product 1",
        )
        val item2 = OrderItem.create(
            orderId = 1L,
            productId = 2L,
            quantity = 1,
            price = BigDecimal("20000"),
            productName = "Product 2",
        )
        val items = listOf(item1, item2)
        val totalDiscount = BigDecimal("10000")
        val totalAmount = BigDecimal("30000")

        // Act
        DiscountDistributer.distributeDiscount(items, totalDiscount, totalAmount)

        // Assert
        assertEquals(BigDecimal("3333"), item1.discountAmount)
        assertEquals(BigDecimal("6667"), item2.discountAmount)
        assertEquals(totalDiscount, item1.discountAmount + item2.discountAmount)
    }

    @DisplayName("빈 목록에는 할인을 적용하지 않는다")
    @Test
    fun distributeDiscount_doesNothing_whenListIsEmpty() {
        // Arrange & Act
        DiscountDistributer.distributeDiscount(
            emptyList(),
            BigDecimal("10000"),
            BigDecimal("30000"),
        )

        // Assert - 예외 없음
    }

    @DisplayName("총액이 0 이하면 예외 발생")
    @Test
    fun distributeDiscount_throwsException_whenTotalAmountIsZeroOrNegative() {
        // Arrange
        val item = OrderItem.create(
            orderId = 1L,
            productId = 1L,
            quantity = 1,
            price = BigDecimal("10000"),
            productName = "Product",
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            DiscountDistributer.distributeDiscount(
                listOf(item),
                BigDecimal("1000"),
                BigDecimal.ZERO,
            )
        }
    }
}
