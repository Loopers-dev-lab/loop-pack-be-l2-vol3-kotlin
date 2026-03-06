package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductModel")
class ProductModelTest {

    companion object {
        private const val VALID_NAME = "감성 티셔츠"
        private const val VALID_PRICE = 25000L
        private const val VALID_BRAND_ID = 1L
        private const val VALID_DESCRIPTION = "부드러운 면 소재의 감성 티셔츠"
        private const val VALID_THUMBNAIL_URL = "https://example.com/product.png"
        private const val VALID_STOCK_QUANTITY = 100
    }

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면 ProductModel이 생성되고, 기본 상태는 SELLING/VISIBLE이다")
        @Test
        fun createsProductModel_whenAllFieldsAreValid() {
            // arrange & act
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                description = VALID_DESCRIPTION,
                thumbnailImageUrl = VALID_THUMBNAIL_URL,
                stockQuantity = VALID_STOCK_QUANTITY,
            )

            // assert
            assertThat(product.name).isEqualTo(VALID_NAME)
            assertThat(product.price).isEqualTo(VALID_PRICE)
            assertThat(product.brandId).isEqualTo(VALID_BRAND_ID)
            assertThat(product.description).isEqualTo(VALID_DESCRIPTION)
            assertThat(product.thumbnailImageUrl).isEqualTo(VALID_THUMBNAIL_URL)
            assertThat(product.stockQuantity).isEqualTo(VALID_STOCK_QUANTITY)
            assertThat(product.likesCount).isEqualTo(0L)
            assertThat(product.saleStatus).isEqualTo(SaleStatus.SELLING)
            assertThat(product.displayStatus).isEqualTo(DisplayStatus.VISIBLE)
        }

        @DisplayName("선택 필드가 null이어도 정상 생성된다")
        @Test
        fun createsProductModel_whenOptionalFieldsAreNull() {
            // arrange & act
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
            )

            // assert
            assertThat(product.description).isNull()
            assertThat(product.thumbnailImageUrl).isNull()
            assertThat(product.stockQuantity).isEqualTo(0)
            assertThat(product.likesCount).isEqualTo(0L)
        }
    }

    @DisplayName("이름 검증")
    @Nested
    inner class NameValidation {
        @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsBlank() {
            // arrange & act & assert
            assertThatThrownBy {
                ProductModel(name = "", price = VALID_PRICE, brandId = VALID_BRAND_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 공백만으로 이루어져 있으면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsOnlyWhitespace() {
            // arrange & act & assert
            assertThatThrownBy {
                ProductModel(name = "   ", price = VALID_PRICE, brandId = VALID_BRAND_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 200자를 초과하면 예외가 발생한다")
        @Test
        fun throwsException_whenNameExceeds200Characters() {
            // arrange & act & assert
            assertThatThrownBy {
                ProductModel(name = "가".repeat(201), price = VALID_PRICE, brandId = VALID_BRAND_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 정확히 200자이면 정상 생성된다")
        @Test
        fun createsProductModel_whenNameIsExactly200Characters() {
            // arrange & act
            val product = ProductModel(
                name = "가".repeat(200),
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
            )

            // assert
            assertThat(product.name).hasSize(200)
        }
    }

    @DisplayName("가격 검증")
    @Nested
    inner class PriceValidation {
        @DisplayName("가격이 음수이면 예외가 발생한다")
        @Test
        fun throwsException_whenPriceIsNegative() {
            // arrange & act & assert
            assertThatThrownBy {
                ProductModel(name = VALID_NAME, price = -1L, brandId = VALID_BRAND_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 0이면 정상 생성된다")
        @Test
        fun createsProductModel_whenPriceIsZero() {
            // arrange & act
            val product = ProductModel(name = VALID_NAME, price = 0L, brandId = VALID_BRAND_ID)

            // assert
            assertThat(product.price).isEqualTo(0L)
        }
    }

    @DisplayName("재고 수량 검증")
    @Nested
    inner class StockQuantityValidation {
        @DisplayName("재고 수량이 음수이면 예외가 발생한다")
        @Test
        fun throwsException_whenStockQuantityIsNegative() {
            // arrange & act & assert
            assertThatThrownBy {
                ProductModel(
                    name = VALID_NAME,
                    price = VALID_PRICE,
                    brandId = VALID_BRAND_ID,
                    stockQuantity = -1,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("좋아요 수 검증")
    @Nested
    inner class LikesCountValidation {
        @DisplayName("좋아요 수가 음수이면 예외가 발생한다")
        @Test
        fun throwsException_whenLikesCountIsNegative() {
            // arrange & act & assert
            assertThatThrownBy {
                ProductModel(
                    name = VALID_NAME,
                    price = VALID_PRICE,
                    brandId = VALID_BRAND_ID,
                    likesCount = -1L,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("재고 차감")
    @Nested
    inner class DecreaseStock {
        @DisplayName("정상적으로 재고를 차감한다 (10 → 7)")
        @Test
        fun decreasesStock_whenSufficientQuantity() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                stockQuantity = 10,
            )

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.stockQuantity).isEqualTo(7)
        }

        @DisplayName("재고를 정확히 소진한다 (3개 보유, 3개 차감)")
        @Test
        fun decreasesStockToZero_whenExactQuantity() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                stockQuantity = 3,
            )

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.stockQuantity).isEqualTo(0)
        }

        @DisplayName("재고 부족 시 예외가 발생한다 (3개 보유, 5개 차감)")
        @Test
        fun throwsException_whenInsufficientStock() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                stockQuantity = 3,
            )

            // act & assert
            assertThatThrownBy {
                product.decreaseStock(5)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("재고가 부족합니다")
        }

        @DisplayName("차감 수량이 0 이하이면 예외가 발생한다")
        @Test
        fun throwsException_whenQuantityIsZeroOrNegative() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                stockQuantity = 10,
            )

            // act & assert
            assertThatThrownBy {
                product.decreaseStock(0)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @DisplayName("재고 증가")
    @Nested
    inner class IncreaseStock {
        @DisplayName("정상적으로 재고를 증가시킨다")
        @Test
        fun increasesStock_whenValidQuantity() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                stockQuantity = 10,
            )

            // act
            product.increaseStock(5)

            // assert
            assertThat(product.stockQuantity).isEqualTo(15)
        }

        @DisplayName("증가 수량이 0 이하이면 예외가 발생한다")
        @Test
        fun throwsException_whenQuantityIsZeroOrNegative() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                stockQuantity = 10,
            )

            // act & assert
            assertThatThrownBy {
                product.increaseStock(0)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @DisplayName("좋아요 수 증감")
    @Nested
    inner class LikesCount {
        @DisplayName("좋아요 수를 증가시킨다")
        @Test
        fun incrementsLikesCount() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
            )

            // act
            product.incrementLikesCount()

            // assert
            assertThat(product.likesCount).isEqualTo(1L)
        }

        @DisplayName("좋아요 수를 감소시킨다")
        @Test
        fun decrementsLikesCount() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                likesCount = 5L,
            )

            // act
            product.decrementLikesCount()

            // assert
            assertThat(product.likesCount).isEqualTo(4L)
        }

        @DisplayName("좋아요 수가 0일 때 감소 시도하면 0을 유지한다 (음수 방지)")
        @Test
        fun keepsZero_whenDecrementsAtZero() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                likesCount = 0L,
            )

            // act
            product.decrementLikesCount()

            // assert
            assertThat(product.likesCount).isEqualTo(0L)
        }
    }
}
