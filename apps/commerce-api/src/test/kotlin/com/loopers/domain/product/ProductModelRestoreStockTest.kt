package com.loopers.domain.product

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductModelRestoreStockTest {
    private fun createProduct(stockQuantity: Int = 10) = ProductModel(
        brandId = 1L,
        name = "감성 티셔츠",
        description = "좋은 상품입니다.",
        price = 39000,
        stockQuantity = stockQuantity,
        imageUrl = "https://example.com/product.jpg",
    )

    @DisplayName("재고를 복원할 때,")
    @Nested
    inner class RestoreStock {
        @DisplayName("양수 수량이면, 재고가 증가한다.")
        @Test
        fun restoresStock_whenPositiveQuantity() {
            // arrange
            val product = createProduct(stockQuantity = 5)

            // act
            val restored = product.restoreStock(3)

            // assert
            assertThat(restored.stockQuantity).isEqualTo(8)
        }

        @DisplayName("수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZero() {
            // arrange
            val product = createProduct(stockQuantity = 5)

            // act & assert
            val result = assertThrows<CoreException> { product.restoreStock(0) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsNegative() {
            // arrange
            val product = createProduct(stockQuantity = 5)

            // act & assert
            val result = assertThrows<CoreException> { product.restoreStock(-1) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
