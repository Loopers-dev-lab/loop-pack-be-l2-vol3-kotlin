package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ProductTest {

    companion object {
        private const val TEST_BRAND_ID = 1L
        private const val TEST_NAME = "에어맥스 90"
        private val TEST_PRICE = BigDecimal("129000")
        private const val TEST_STOCK = 100
        private const val TEST_DESCRIPTION = "나이키 에어맥스 90"
        private const val TEST_IMAGE_URL = "https://example.com/airmax90.jpg"
    }

    private fun createProduct(
        brandId: Long = TEST_BRAND_ID,
        name: String = TEST_NAME,
        price: BigDecimal = TEST_PRICE,
        stock: Int = TEST_STOCK,
        description: String? = TEST_DESCRIPTION,
        imageUrl: String? = TEST_IMAGE_URL,
    ): Product {
        return Product(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            description = description,
            imageUrl = imageUrl,
        )
    }

    @DisplayName("상품을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("정상적인 정보가 주어지면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenValidInfoProvided() {
            // act
            val product = createProduct()

            // assert
            assertAll(
                { assertThat(product.brandId).isEqualTo(TEST_BRAND_ID) },
                { assertThat(product.name).isEqualTo(TEST_NAME) },
                { assertThat(product.price).isEqualByComparingTo(TEST_PRICE) },
                { assertThat(product.stock).isEqualTo(TEST_STOCK) },
                { assertThat(product.description).isEqualTo(TEST_DESCRIPTION) },
                { assertThat(product.imageUrl).isEqualTo(TEST_IMAGE_URL) },
            )
        }

        @DisplayName("description과 imageUrl이 null이어도, 상품이 생성된다.")
        @Test
        fun createsProduct_whenOptionalFieldsAreNull() {
            // act
            val product = createProduct(description = null, imageUrl = null)

            // assert
            assertAll(
                { assertThat(product.description).isNull() },
                { assertThat(product.imageUrl).isNull() },
            )
        }

        @DisplayName("name이 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameIsBlank() {
            // act
            val exception = assertThrows<CoreException> {
                createProduct(name = "  ")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("name이 200자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameExceedsMaxLength() {
            // act
            val exception = assertThrows<CoreException> {
                createProduct(name = "a".repeat(201))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("price가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenPriceIsNegative() {
            // act
            val exception = assertThrows<CoreException> {
                createProduct(price = BigDecimal("-1"))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("price가 0이면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenPriceIsZero() {
            // act
            val product = createProduct(price = BigDecimal.ZERO)

            // assert
            assertThat(product.price).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @DisplayName("stock이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenStockIsNegative() {
            // act
            val exception = assertThrows<CoreException> {
                createProduct(stock = -1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("stock이 0이면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenStockIsZero() {
            // act
            val product = createProduct(stock = 0)

            // assert
            assertThat(product.stock).isEqualTo(0)
        }
    }

    @DisplayName("상품을 수정할 때,")
    @Nested
    inner class Update {

        @DisplayName("정상적인 정보가 주어지면, 상품이 수정된다.")
        @Test
        fun updatesProduct_whenValidInfoProvided() {
            // arrange
            val product = createProduct()

            // act
            product.update(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = "나이키 에어포스 1",
                imageUrl = "https://example.com/airforce1.jpg",
            )

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo("에어포스 1") },
                { assertThat(product.price).isEqualByComparingTo(BigDecimal("139000")) },
                { assertThat(product.stock).isEqualTo(50) },
                { assertThat(product.description).isEqualTo("나이키 에어포스 1") },
                { assertThat(product.imageUrl).isEqualTo("https://example.com/airforce1.jpg") },
            )
        }

        @DisplayName("brandId는 변경되지 않는다.")
        @Test
        fun doesNotChangeBrandId_whenUpdated() {
            // arrange
            val product = createProduct(brandId = 1L)

            // act
            product.update(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = null,
                imageUrl = null,
            )

            // assert
            assertThat(product.brandId).isEqualTo(1L)
        }

        @DisplayName("name이 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameIsBlank() {
            // arrange
            val product = createProduct()

            // act
            val exception = assertThrows<CoreException> {
                product.update(name = "  ", price = TEST_PRICE, stock = TEST_STOCK, description = null, imageUrl = null)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("재고를 확인할 때,")
    @Nested
    inner class HasEnoughStock {

        @DisplayName("재고가 충분하면, true를 반환한다.")
        @Test
        fun returnsTrue_whenStockIsSufficient() {
            // arrange
            val product = createProduct(stock = 10)

            // act & assert
            assertThat(product.hasEnoughStock(10)).isTrue()
        }

        @DisplayName("재고가 부족하면, false를 반환한다.")
        @Test
        fun returnsFalse_whenStockIsInsufficient() {
            // arrange
            val product = createProduct(stock = 5)

            // act & assert
            assertThat(product.hasEnoughStock(6)).isFalse()
        }

        @DisplayName("재고가 0이면, false를 반환한다.")
        @Test
        fun returnsFalse_whenStockIsZero() {
            // arrange
            val product = createProduct(stock = 0)

            // act & assert
            assertThat(product.hasEnoughStock(1)).isFalse()
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class DecreaseStock {

        @DisplayName("재고가 충분하면, 차감된다.")
        @Test
        fun decreasesStock_whenSufficient() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.stock).isEqualTo(7)
        }

        @DisplayName("재고를 전부 차감할 수 있다.")
        @Test
        fun decreasesStock_toZero() {
            // arrange
            val product = createProduct(stock = 5)

            // act
            product.decreaseStock(5)

            // assert
            assertThat(product.stock).isEqualTo(0)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenStockIsInsufficient() {
            // arrange
            val product = createProduct(stock = 5)

            // act
            val exception = assertThrows<CoreException> {
                product.decreaseStock(6)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("차감 수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenQuantityIsZero() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            val exception = assertThrows<CoreException> {
                product.decreaseStock(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("차감 수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenQuantityIsNegative() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            val exception = assertThrows<CoreException> {
                product.decreaseStock(-1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("재고를 예약할 때,")
    @Nested
    inner class Reserve {

        @DisplayName("재고가 충분하면, true를 반환하고 재고가 차감된다.")
        @Test
        fun reservesStock_whenSufficientStock() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            val result = product.reserve(3)

            // assert
            assertAll(
                { assertThat(result).isTrue() },
                { assertThat(product.stock).isEqualTo(7) },
            )
        }

        @DisplayName("재고가 부족하면, false를 반환하고 재고가 변경되지 않는다.")
        @Test
        fun returnsFalse_whenInsufficientStock() {
            // arrange
            val product = createProduct(stock = 2)

            // act
            val result = product.reserve(3)

            // assert
            assertAll(
                { assertThat(result).isFalse() },
                { assertThat(product.stock).isEqualTo(2) },
            )
        }

        @DisplayName("수량이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityNotPositive() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            val exception = assertThrows<CoreException> {
                product.reserve(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("연속 예약 시 재고가 누적 차감된다.")
        @Test
        fun tracksStockAcrossMultipleReserves() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            val first = product.reserve(3)
            val second = product.reserve(3)
            val third = product.reserve(5)

            // assert
            assertAll(
                { assertThat(first).isTrue() },
                { assertThat(second).isTrue() },
                { assertThat(third).isFalse() },
                { assertThat(product.stock).isEqualTo(4) },
            )
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면, deletedAt이 설정된다.")
        @Test
        fun setDeletedAt_whenDeleteCalled() {
            // arrange
            val product = createProduct()

            // act
            product.delete()

            // assert
            assertThat(product.isDeleted()).isTrue()
        }

        @DisplayName("이미 삭제된 상품을 다시 삭제해도, 정상 동작한다.")
        @Test
        fun remainsDeleted_whenAlreadyDeleted() {
            // arrange
            val product = createProduct()
            product.delete()

            // act
            product.delete()

            // assert
            assertThat(product.isDeleted()).isTrue()
        }

        @DisplayName("삭제되지 않은 상품은 isDeleted가 false이다.")
        @Test
        fun returnsFalse_whenNotDeleted() {
            // arrange
            val product = createProduct()

            // act & assert
            assertThat(product.isDeleted()).isFalse()
        }
    }
}
