package com.loopers.domain.catalog

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ProductModelTest {
    companion object {
        private const val DEFAULT_BRAND_ID = 1L
        private const val DEFAULT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")
    }

    private fun createProductModel(
        brandId: Long = DEFAULT_BRAND_ID,
        name: String = DEFAULT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_PRICE,
    ) = ProductModel(brandId = brandId, name = name, quantity = quantity, price = price)

    @DisplayName("생성")
    @Nested
    inner class Create {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsProductModelWhenValidParametersAreProvided() {
            // act
            val product = createProductModel()

            // assert
            assertAll(
                { assertThat(product.brandId).isEqualTo(DEFAULT_BRAND_ID) },
                { assertThat(product.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(product.quantity).isEqualTo(DEFAULT_QUANTITY) },
                { assertThat(product.price).isEqualByComparingTo(DEFAULT_PRICE) },
            )
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenNameIsBlank() {
            // act & assert
            val result = assertThrows<CoreException> {
                createProductModel(name = "   ")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenPriceIsZeroOrNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createProductModel(price = BigDecimal.ZERO)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 0 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityIsNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createProductModel(quantity = -1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 0이면, 정상적으로 생성된다.")
        @Test
        fun createsProductModelWhenQuantityIsZero() {
            // act
            val product = createProductModel(quantity = 0)

            // assert
            assertThat(product.quantity).isEqualTo(0)
        }
    }

    @DisplayName("재고 차감")
    @Nested
    inner class DecreaseStock {
        @DisplayName("유효한 수량이 주어지면, 재고가 차감된다.")
        @Test
        fun decreasesStockWhenValidQuantityIsProvided() {
            // arrange
            val product = createProductModel(quantity = 10)
            val expectedQuantity = 7

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.quantity).isEqualTo(expectedQuantity)
        }

        @DisplayName("재고와 동일한 수량을 차감하면, 재고가 0이 된다.")
        @Test
        fun decreasesStockToZeroWhenQuantityEqualsStock() {
            // arrange
            val product = createProductModel(quantity = 5)

            // act
            product.decreaseStock(5)

            // assert
            assertThat(product.quantity).isEqualTo(0)
        }

        @DisplayName("재고보다 많은 수량을 차감하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityExceedsStock() {
            // arrange
            val product = createProductModel(quantity = 3)

            // act & assert
            val result = assertThrows<CoreException> {
                product.decreaseStock(5)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("차감 수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityIsZero() {
            // arrange
            val product = createProductModel(quantity = 10)

            // act & assert
            val result = assertThrows<CoreException> {
                product.decreaseStock(0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("차감 수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityIsNegative() {
            // arrange
            val product = createProductModel(quantity = 10)

            // act & assert
            val result = assertThrows<CoreException> {
                product.decreaseStock(-1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("수정")
    @Nested
    inner class Update {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesProductModelWhenValidParametersAreProvided() {
            // arrange
            val product = createProductModel()
            val expectedName = "에어포스 1"
            val expectedQuantity = 50
            val expectedPrice = BigDecimal("99000")

            // act
            product.update(newName = expectedName, newQuantity = expectedQuantity, newPrice = expectedPrice)

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo(expectedName) },
                { assertThat(product.quantity).isEqualTo(expectedQuantity) },
                { assertThat(product.price).isEqualByComparingTo(expectedPrice) },
            )
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenNameIsBlank() {
            // arrange
            val product = createProductModel()

            // act & assert
            val result = assertThrows<CoreException> {
                product.update(newName = "   ", newQuantity = DEFAULT_QUANTITY, newPrice = DEFAULT_PRICE)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenPriceIsZeroOrNegative() {
            // arrange
            val product = createProductModel()

            // act & assert
            val result = assertThrows<CoreException> {
                product.update(newName = DEFAULT_NAME, newQuantity = DEFAULT_QUANTITY, newPrice = BigDecimal("-1"))
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 0 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityIsNegative() {
            // arrange
            val product = createProductModel()

            // act & assert
            val result = assertThrows<CoreException> {
                product.update(newName = DEFAULT_NAME, newQuantity = -1, newPrice = DEFAULT_PRICE)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
