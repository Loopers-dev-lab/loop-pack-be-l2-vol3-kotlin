package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductTest {
    @DisplayName("상품을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("모든 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsProduct_whenAllFieldsAreProvided() {
            // arrange & act
            val product = Product(
                brandId = 1L,
                name = "에어맥스",
                description = "나이키 에어맥스",
                price = 199000L,
                stockQuantity = 100,
            )

            // assert
            assertAll(
                { assertThat(product.brandId).isEqualTo(1L) },
                { assertThat(product.name).isEqualTo("에어맥스") },
                { assertThat(product.description).isEqualTo("나이키 에어맥스") },
                { assertThat(product.price).isEqualTo(199000L) },
                { assertThat(product.stockQuantity).isEqualTo(100) },
                { assertThat(product.likeCount).isEqualTo(0) },
            )
        }
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    inner class DecreaseStock {
        @DisplayName("충분한 재고가 있으면, 재고가 차감된다.")
        @Test
        fun decreasesStock_whenSufficientStock() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "설명", price = 199000L, stockQuantity = 100)

            // act
            product.decreaseStock(30)

            // assert
            assertThat(product.stockQuantity).isEqualTo(70)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenInsufficientStock() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "설명", price = 199000L, stockQuantity = 5)

            // act
            val exception = assertThrows<CoreException> {
                product.decreaseStock(10)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("좋아요 수를 변경할 때, ")
    @Nested
    inner class LikeCount {
        @DisplayName("좋아요 수를 증가시키면, 1 증가한다.")
        @Test
        fun increasesLikeCount() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "설명", price = 199000L, stockQuantity = 100)

            // act
            product.increaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @DisplayName("좋아요 수를 감소시키면, 1 감소한다.")
        @Test
        fun decreasesLikeCount() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "설명", price = 199000L, stockQuantity = 100)
            product.increaseLikeCount()

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(0)
        }

        @DisplayName("좋아요 수가 0일 때 감소시키면, 0을 유지한다.")
        @Test
        fun doesNotGoBelowZero() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "설명", price = 199000L, stockQuantity = 100)

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(0)
        }
    }

    @DisplayName("상품을 수정할 때, ")
    @Nested
    inner class Update {
        @DisplayName("새 정보가 주어지면, 해당 값으로 수정된다.")
        @Test
        fun updatesProduct_whenNewValuesAreProvided() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "설명", price = 199000L, stockQuantity = 100)

            // act
            product.update(name = "에어포스", description = "새 설명", price = 149000L, stockQuantity = 50)

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo("에어포스") },
                { assertThat(product.description).isEqualTo("새 설명") },
                { assertThat(product.price).isEqualTo(149000L) },
                { assertThat(product.stockQuantity).isEqualTo(50) },
            )
        }
    }
}
