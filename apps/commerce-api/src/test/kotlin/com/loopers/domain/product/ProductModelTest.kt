package com.loopers.domain.product

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductModelTest {
    private fun createProduct(
        brandId: Long = 1L,
        name: String = "감성 티셔츠",
        description: String = "좋은 상품입니다.",
        price: Long = 39000,
        stockQuantity: Int = 100,
        imageUrl: String = "https://example.com/product.jpg",
    ) = ProductModel(
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        imageUrl = imageUrl,
    )

    @DisplayName("상품 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("모든 값이 유효하면, 정상적으로 생성된다.")
        @Test
        fun createsProduct_whenAllFieldsAreValid() {
            val product = createProduct()
            assertAll(
                { assertThat(product.brandId).isEqualTo(1L) },
                { assertThat(product.name).isEqualTo("감성 티셔츠") },
                { assertThat(product.price).isEqualTo(39000L) },
                { assertThat(product.stockQuantity).isEqualTo(100) },
                { assertThat(product.likeCount).isEqualTo(0) },
                { assertThat(product.status).isEqualTo(ProductStatus.ACTIVE) },
            )
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class DeductStock {
        @DisplayName("충분한 재고가 있으면, 정상적으로 차감된다.")
        @Test
        fun deductsStock_whenSufficientQuantity() {
            val product = createProduct(stockQuantity = 10)
            val deducted = product.deductStock(3)
            assertThat(deducted.stockQuantity).isEqualTo(7)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenInsufficientStock() {
            val product = createProduct(stockQuantity = 2)
            val result = assertThrows<CoreException> { product.deductStock(3) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("재고와 동일한 수량을 차감하면, 재고가 0이 된다.")
        @Test
        fun deductsToZero_whenExactQuantity() {
            val product = createProduct(stockQuantity = 5)
            val deducted = product.deductStock(5)
            assertThat(deducted.stockQuantity).isEqualTo(0)
        }

        @DisplayName("수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZero() {
            val product = createProduct(stockQuantity = 10)
            val result = assertThrows<CoreException> { product.deductStock(0) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsNegative() {
            val product = createProduct(stockQuantity = 10)
            val result = assertThrows<CoreException> { product.deductStock(-1) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("상품을 수정할 때,")
    @Nested
    inner class Update {
        @DisplayName("유효한 값이 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesProduct_whenValidFields() {
            val product = createProduct()
            val updated = product.update(
                name = "새 상품",
                description = "새 설명",
                price = 50000,
                stockQuantity = 200,
                imageUrl = "https://example.com/new.jpg",
            )
            assertAll(
                { assertThat(updated.name).isEqualTo("새 상품") },
                { assertThat(updated.description).isEqualTo("새 설명") },
                { assertThat(updated.price).isEqualTo(50000L) },
                { assertThat(updated.stockQuantity).isEqualTo(200) },
                { assertThat(updated.imageUrl).isEqualTo("https://example.com/new.jpg") },
            )
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class Delete {
        @DisplayName("삭제하면, 상태가 DELETED로 변경된다.")
        @Test
        fun changesStatusToDeleted() {
            val product = createProduct()
            val deleted = product.delete()
            assertAll(
                { assertThat(deleted.status).isEqualTo(ProductStatus.DELETED) },
                { assertThat(deleted.isDeleted()).isTrue() },
            )
        }
    }
}
