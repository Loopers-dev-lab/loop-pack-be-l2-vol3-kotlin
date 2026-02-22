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

    @DisplayName("상품 생성할 때,")
    @Nested
    inner class Create {
        private val name = "에어맥스"
        private val description = "러닝화"
        private val price = 159000L
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
                Product(
                    name = name,
                    description = description,
                    price = -1,
                    likes = likes,
                    stockQuantity = stockQuantity,
                    brandId = brandId,
                )
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
}
