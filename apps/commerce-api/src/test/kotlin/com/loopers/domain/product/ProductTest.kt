package com.loopers.domain.product

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductTest {

    @DisplayName("상품 생성할 때,")
    @Nested
    inner class Create {
        private val name = "에어맥스"
        private val description = "러닝화"
        private val price = Money.of(159000L)
        private val likes = 10
        private val stockQuantity = 100
        private val brandId = 1L

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsProduct_whenValidValuesProvided() {
            // arrange & act
            val product = Product(
                name = name,
                description = description,
                price = price,
                likes = likes,
                stockQuantity = stockQuantity,
                brandId = brandId,
            )

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo(name) },
                { assertThat(product.description).isEqualTo(description) },
                { assertThat(product.price).isEqualTo(price) },
                { assertThat(product.likes).isEqualTo(likes) },
                { assertThat(product.stockQuantity).isEqualTo(stockQuantity) },
                { assertThat(product.brandId).isEqualTo(brandId) },
            )
        }

        @DisplayName("설명이 null이면, 정상적으로 생성된다.")
        @Test
        fun createsProduct_whenDescriptionIsNull() {
            // arrange & act
            val product = Product(
                name = name,
                description = null,
                price = price,
                likes = likes,
                stockQuantity = stockQuantity,
                brandId = brandId,
            )

            // assert
            assertThat(product.description).isNull()
        }

        @DisplayName("이름이 빈칸이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // act
            val exception = assertThrows<CoreException> {
                Product(
                    name = "  ",
                    description = description,
                    price = price,
                    likes = likes,
                    stockQuantity = stockQuantity,
                    brandId = brandId,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPriceIsNegative() {
            // act
            val exception = assertThrows<CoreException> {
                Money.of(-1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("재고 수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenStockQuantityIsNegative() {
            // act
            val exception = assertThrows<CoreException> {
                Product(
                    name = name,
                    description = description,
                    price = price,
                    likes = likes,
                    stockQuantity = -1,
                    brandId = brandId,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    private fun createProduct(likes: Int = 10): Product {
        return Product(
            name = "에어맥스",
            description = "러닝화",
            price = Money.of(159000L),
            likes = likes,
            stockQuantity = 100,
            brandId = 1L,
        )
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class DeductStock {

        @DisplayName("차감 수량만큼 재고가 감소한다.")
        @Test
        fun deductsStockByGivenQuantity() {
            // arrange
            val product = createProduct()

            // act
            product.deductStock(10)

            // assert
            assertThat(product.stockQuantity).isEqualTo(90)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenInsufficientStock() {
            // arrange
            val product = createProduct()

            // act
            val exception = assertThrows<CoreException> {
                product.deductStock(101)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("차감 수량이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZeroOrNegative() {
            // arrange
            val product = createProduct()

            // act
            val exception = assertThrows<CoreException> {
                product.deductStock(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("좋아요 수를 증가시킬 때,")
    @Nested
    inner class IncreaseLikeCount {

        @DisplayName("좋아요 수가 1 증가한다.")
        @Test
        fun increasesLikeCountByOne() {
            // arrange
            val product = createProduct(likes = 10)

            // act
            product.increaseLikeCount()

            // assert
            assertThat(product.likes).isEqualTo(11)
        }
    }

    @DisplayName("좋아요 수를 감소시킬 때,")
    @Nested
    inner class DecreaseLikeCount {

        @DisplayName("좋아요 수가 1 감소한다.")
        @Test
        fun decreasesLikeCountByOne() {
            // arrange
            val product = createProduct(likes = 10)

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likes).isEqualTo(9)
        }

        @DisplayName("좋아요 수가 0이면, 0을 유지한다.")
        @Test
        fun doesNotDecreaseBelow_zero() {
            // arrange
            val product = createProduct(likes = 0)

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likes).isEqualTo(0)
        }
    }
}
