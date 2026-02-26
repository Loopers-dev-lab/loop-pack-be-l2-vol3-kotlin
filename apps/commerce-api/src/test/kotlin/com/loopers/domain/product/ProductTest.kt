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

    private fun createProduct(
        brandId: Long = 1L,
        name: String = "에어맥스 90",
        description: String? = "나이키 에어맥스 90",
        price: Long = 139000,
        stockQuantity: Int = 100,
        displayYn: Boolean = true,
        imageUrl: String? = "https://example.com/airmax90.png",
    ): Product = Product(
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        displayYn = displayYn,
        imageUrl = imageUrl,
    )

    @Nested
    inner class CreateProduct {

        @Test
        @DisplayName("모든 정보가 올바르면 정상적으로 생성된다")
        fun success() {
            // arrange & act
            val product = createProduct()

            // assert
            assertAll(
                { assertThat(product.brandId).isEqualTo(1L) },
                { assertThat(product.name).isEqualTo("에어맥스 90") },
                { assertThat(product.price).isEqualTo(139000) },
                { assertThat(product.stockQuantity).isEqualTo(100) },
                { assertThat(product.likeCount).isEqualTo(0) },
                { assertThat(product.status).isEqualTo(ProductStatus.ACTIVE) },
                { assertThat(product.displayYn).isTrue() },
            )
        }

        @Test
        @DisplayName("상품명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createProduct(name = "   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("상품명이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        fun nameEmptyThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createProduct(name = "")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("가격이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativePriceThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createProduct(price = -1)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("재고 수량이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeStockThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createProduct(stockQuantity = -1)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("가격이 0이면 정상적으로 생성된다")
        fun zeroPriceSuccess() {
            // arrange & act
            val product = createProduct(price = 0)

            // assert
            assertThat(product.price).isEqualTo(0)
        }

        @Test
        @DisplayName("재고 수량이 0이면 정상적으로 생성된다")
        fun zeroStockSuccess() {
            // arrange & act
            val product = createProduct(stockQuantity = 0)

            // assert
            assertThat(product.stockQuantity).isEqualTo(0)
        }

        @Test
        @DisplayName("설명과 이미지URL이 null이어도 정상적으로 생성된다")
        fun nullableFieldsSuccess() {
            // arrange & act
            val product = createProduct(description = null, imageUrl = null)

            // assert
            assertAll(
                { assertThat(product.description).isNull() },
                { assertThat(product.imageUrl).isNull() },
            )
        }
    }

    @Nested
    inner class UpdateProduct {

        @Test
        @DisplayName("올바른 정보로 수정하면 필드가 변경된다")
        fun success() {
            // arrange
            val product = createProduct()

            // act
            product.update(
                name = "에어포스 1",
                description = "클래식 스니커즈",
                price = 119000,
                stockQuantity = 50,
                status = ProductStatus.INACTIVE,
                displayYn = false,
                imageUrl = "https://example.com/af1.png",
            )

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo("에어포스 1") },
                { assertThat(product.description).isEqualTo("클래식 스니커즈") },
                { assertThat(product.price).isEqualTo(119000) },
                { assertThat(product.stockQuantity).isEqualTo(50) },
                { assertThat(product.status).isEqualTo(ProductStatus.INACTIVE) },
                { assertThat(product.displayYn).isFalse() },
                { assertThat(product.imageUrl).isEqualTo("https://example.com/af1.png") },
            )
        }

        @Test
        @DisplayName("수정 시 상품명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            // arrange
            val product = createProduct()

            // act
            val result = assertThrows<CoreException> {
                product.update(
                    name = "   ",
                    description = null,
                    price = 10000,
                    stockQuantity = 10,
                    status = ProductStatus.ACTIVE,
                    displayYn = true,
                    imageUrl = null,
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("수정 시 가격이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativePriceThrowsBadRequest() {
            // arrange
            val product = createProduct()

            // act
            val result = assertThrows<CoreException> {
                product.update(
                    name = "에어맥스",
                    description = null,
                    price = -1,
                    stockQuantity = 10,
                    status = ProductStatus.ACTIVE,
                    displayYn = true,
                    imageUrl = null,
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("수정 시 재고 수량이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeStockThrowsBadRequest() {
            // arrange
            val product = createProduct()

            // act
            val result = assertThrows<CoreException> {
                product.update(
                    name = "에어맥스",
                    description = null,
                    price = 10000,
                    stockQuantity = -1,
                    status = ProductStatus.ACTIVE,
                    displayYn = true,
                    imageUrl = null,
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class DecreaseStock {

        @Test
        @DisplayName("충분한 재고가 있으면 재고가 차감된다")
        fun success() {
            // arrange
            val product = createProduct(stockQuantity = 10)

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.stockQuantity).isEqualTo(7)
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        fun insufficientStockThrowsBadRequest() {
            // arrange
            val product = createProduct(stockQuantity = 2)

            // act
            val result = assertThrows<CoreException> {
                product.decreaseStock(3)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("재고와 동일한 수량을 차감하면 재고가 0이 된다")
        fun exactStockSuccess() {
            // arrange
            val product = createProduct(stockQuantity = 5)

            // act
            product.decreaseStock(5)

            // assert
            assertThat(product.stockQuantity).isEqualTo(0)
        }
    }

    @Nested
    inner class IncreaseStock {

        @Test
        @DisplayName("재고가 증가한다")
        fun success() {
            // arrange
            val product = createProduct(stockQuantity = 10)

            // act
            product.increaseStock(5)

            // assert
            assertThat(product.stockQuantity).isEqualTo(15)
        }
    }

    @Nested
    inner class LikeCount {

        @Test
        @DisplayName("좋아요 수가 증가한다")
        fun increaseSuccess() {
            // arrange
            val product = createProduct()

            // act
            product.increaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요 수가 감소한다")
        fun decreaseSuccess() {
            // arrange
            val product = createProduct()
            product.increaseLikeCount()
            product.increaseLikeCount()

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요 수가 0이면 감소해도 0을 유지한다")
        fun decreaseAtZeroStaysZero() {
            // arrange
            val product = createProduct()

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(0)
        }
    }

    @Nested
    inner class IsOrderable {

        @Test
        @DisplayName("ACTIVE 상태이고 전시중이며 재고가 있으면 주문 가능하다")
        fun orderableWhenAllConditionsMet() {
            // arrange & act
            val product = createProduct(stockQuantity = 10, displayYn = true)

            // assert
            assertThat(product.isOrderable()).isTrue()
        }

        @Test
        @DisplayName("INACTIVE 상태이면 주문 불가능하다")
        fun notOrderableWhenInactive() {
            // arrange
            val product = createProduct()
            product.update(
                name = product.name,
                description = product.description,
                price = product.price,
                stockQuantity = product.stockQuantity,
                status = ProductStatus.INACTIVE,
                displayYn = product.displayYn,
                imageUrl = product.imageUrl,
            )

            // assert
            assertThat(product.isOrderable()).isFalse()
        }

        @Test
        @DisplayName("전시 중지 상태이면 주문 불가능하다")
        fun notOrderableWhenNotDisplayed() {
            // arrange & act
            val product = createProduct(displayYn = false)

            // assert
            assertThat(product.isOrderable()).isFalse()
        }

        @Test
        @DisplayName("재고가 0이면 주문 불가능하다")
        fun notOrderableWhenNoStock() {
            // arrange & act
            val product = createProduct(stockQuantity = 0)

            // assert
            assertThat(product.isOrderable()).isFalse()
        }
    }

    @Nested
    inner class SoftDelete {

        @Test
        @DisplayName("소프트 삭제하면 상태가 DELETED로 변경되고 deletedAt이 설정된다")
        fun success() {
            // arrange
            val product = createProduct()

            // act
            product.softDelete()

            // assert
            assertAll(
                { assertThat(product.status).isEqualTo(ProductStatus.DELETED) },
                { assertThat(product.deletedAt).isNotNull() },
            )
        }
    }
}
