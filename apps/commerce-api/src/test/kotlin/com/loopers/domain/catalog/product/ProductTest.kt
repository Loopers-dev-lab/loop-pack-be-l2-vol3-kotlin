package com.loopers.domain.catalog.product

import com.loopers.domain.catalog.product.entity.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ProductTest {

    @Nested
    @DisplayName("Product 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 정보로 생성하면 성공한다 (stock > 0 → ON_SALE)")
        fun create_withValidData_statusOnSale() {
            // arrange & act
            val product = ProductTestFixture.createProduct()

            // assert
            assertThat(product.refBrandId).isEqualTo(ProductTestFixture.DEFAULT_BRAND_ID)
            assertThat(product.name).isEqualTo(ProductTestFixture.DEFAULT_NAME)
            assertThat(product.price).isEqualByComparingTo(ProductTestFixture.DEFAULT_PRICE)
            assertThat(product.stock).isEqualTo(ProductTestFixture.DEFAULT_STOCK)
            assertThat(product.status).isEqualTo(Product.ProductStatus.ON_SALE)
            assertThat(product.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("stock이 0이면 SOLD_OUT으로 생성된다")
        fun create_withZeroStock_statusSoldOut() {
            // arrange & act
            val product = ProductTestFixture.createProduct(stock = 0)

            // assert
            assertThat(product.status).isEqualTo(Product.ProductStatus.SOLD_OUT)
        }

        @Test
        @DisplayName("가격이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNegativePrice_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                ProductTestFixture.createProduct(price = BigDecimal("-1"))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("가격은 0 이상이어야 합니다.")
        }

        @Test
        @DisplayName("재고가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNegativeStock_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                ProductTestFixture.createProduct(stock = -1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("재고는 0 이상이어야 합니다.")
        }
    }

    @Nested
    @DisplayName("decreaseStock 시")
    inner class DecreaseStock {

        @Test
        @DisplayName("재고가 충분하면 차감된다")
        fun decrease_withSufficientStock_success() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 10)

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.stock).isEqualTo(7)
            assertThat(product.status).isEqualTo(Product.ProductStatus.ON_SALE)
        }

        @Test
        @DisplayName("재고가 0이 되면 SOLD_OUT으로 전이된다")
        fun decrease_toZero_statusSoldOut() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 5)

            // act
            product.decreaseStock(5)

            // assert
            assertThat(product.stock).isEqualTo(0)
            assertThat(product.status).isEqualTo(Product.ProductStatus.SOLD_OUT)
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        fun decrease_withInsufficientStock_throwsException() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 2)

            // act & assert
            val exception = assertThrows<CoreException> { product.decreaseStock(3) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("재고가 부족합니다.")
        }

        @Test
        @DisplayName("HIDDEN 상태에서 재고 차감해도 상태가 유지된다")
        fun decrease_whenHidden_statusRemains() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 5)
            product.update(null, null, null, Product.ProductStatus.HIDDEN)

            // act
            product.decreaseStock(5)

            // assert
            assertThat(product.stock).isEqualTo(0)
            assertThat(product.status).isEqualTo(Product.ProductStatus.HIDDEN)
        }
    }

    @Nested
    @DisplayName("increaseStock 시")
    inner class IncreaseStock {

        @Test
        @DisplayName("SOLD_OUT 상태에서 재고가 증가하면 ON_SALE로 전이된다")
        fun increase_fromSoldOut_statusOnSale() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 0)
            assertThat(product.status).isEqualTo(Product.ProductStatus.SOLD_OUT)

            // act
            product.increaseStock(10)

            // assert
            assertThat(product.stock).isEqualTo(10)
            assertThat(product.status).isEqualTo(Product.ProductStatus.ON_SALE)
        }

        @Test
        @DisplayName("HIDDEN 상태에서 재고 증가해도 상태가 유지된다")
        fun increase_whenHidden_statusRemains() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 0)
            product.update(null, null, null, Product.ProductStatus.HIDDEN)

            // act
            product.increaseStock(10)

            // assert
            assertThat(product.stock).isEqualTo(10)
            assertThat(product.status).isEqualTo(Product.ProductStatus.HIDDEN)
        }
    }

    @Nested
    @DisplayName("update 시")
    inner class Update {

        @Test
        @DisplayName("이름, 가격, 재고를 수정할 수 있다")
        fun update_fields_success() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // act
            product.update("에어포스 1", BigDecimal("99000"), 50, null)

            // assert
            assertThat(product.name).isEqualTo("에어포스 1")
            assertThat(product.price).isEqualByComparingTo(BigDecimal("99000"))
            assertThat(product.stock).isEqualTo(50)
        }

        @Test
        @DisplayName("HIDDEN을 명시하면 자동 전이보다 우선한다")
        fun update_withHidden_overridesAutoTransition() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 100)

            // act
            product.update(null, null, 123, Product.ProductStatus.HIDDEN)

            // assert
            assertThat(product.stock).isEqualTo(123)
            assertThat(product.status).isEqualTo(Product.ProductStatus.HIDDEN)
        }

        @Test
        @DisplayName("stock이 0으로 변경되면 SOLD_OUT으로 자동 전이된다")
        fun update_stockToZero_autoTransitionToSoldOut() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 10)

            // act
            product.update(null, null, 0, null)

            // assert
            assertThat(product.status).isEqualTo(Product.ProductStatus.SOLD_OUT)
        }

        @Test
        @DisplayName("stock이 0에서 양수로 변경되면 ON_SALE로 자동 전이된다")
        fun update_stockFromZeroToPositive_autoTransitionToOnSale() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 0)
            assertThat(product.status).isEqualTo(Product.ProductStatus.SOLD_OUT)

            // act
            product.update(null, null, 10, null)

            // assert
            assertThat(product.status).isEqualTo(Product.ProductStatus.ON_SALE)
        }

        @Test
        @DisplayName("HIDDEN 상태에서 status 미명시 시 HIDDEN이 유지된다")
        fun update_hiddenWithoutStatus_remainsHidden() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 10)
            product.update(null, null, null, Product.ProductStatus.HIDDEN)

            // act
            product.update("새이름", null, 0, null)

            // assert
            assertThat(product.name).isEqualTo("새이름")
            assertThat(product.stock).isEqualTo(0)
            assertThat(product.status).isEqualTo(Product.ProductStatus.HIDDEN)
        }

        @Test
        @DisplayName("HIDDEN 상태에서 비-HIDDEN을 명시하면 자동 전이가 적용된다")
        fun update_hiddenWithNonHiddenStatus_appliesAutoTransition() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 10)
            product.update(null, null, null, Product.ProductStatus.HIDDEN)
            assertThat(product.status).isEqualTo(Product.ProductStatus.HIDDEN)

            // act
            product.update(null, null, null, Product.ProductStatus.ON_SALE)

            // assert
            assertThat(product.status).isEqualTo(Product.ProductStatus.ON_SALE)
        }

        @Test
        @DisplayName("stock=0이고 ON_SALE을 명시해도 SOLD_OUT으로 자동 전환된다")
        fun update_zeroStockWithOnSale_autoTransitionToSoldOut() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 10)

            // act
            product.update(null, null, 0, Product.ProductStatus.ON_SALE)

            // assert
            assertThat(product.status).isEqualTo(Product.ProductStatus.SOLD_OUT)
        }

        @Test
        @DisplayName("가격이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun update_withNegativePrice_throwsException() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // act & assert
            val exception = assertThrows<CoreException> {
                product.update(null, BigDecimal("-1"), null, null)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("재고가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun update_withNegativeStock_throwsException() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // act & assert
            val exception = assertThrows<CoreException> {
                product.update(null, null, -1, null)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("likeCount 변경 시")
    inner class LikeCount {

        @Test
        @DisplayName("increaseLikeCount 호출 시 1 증가한다")
        fun increaseLikeCount_increments() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // act
            product.increaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("decreaseLikeCount 호출 시 1 감소한다")
        fun decreaseLikeCount_decrements() {
            // arrange
            val product = ProductTestFixture.createProduct()
            product.increaseLikeCount()
            product.increaseLikeCount()

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("likeCount가 0일 때 decrease해도 0을 유지한다")
        fun decreaseLikeCount_atZero_remainsZero() {
            // arrange
            val product = ProductTestFixture.createProduct()
            assertThat(product.likeCount).isEqualTo(0)

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("isActive 확인 시")
    inner class IsActiveTest {

        @Test
        @DisplayName("ON_SALE 상태이고 삭제되지 않은 상품은 true를 반환한다")
        fun isActive_onSale_returnsTrue() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // assert
            assertThat(product.isActive()).isTrue()
        }

        @Test
        @DisplayName("SOLD_OUT 상태이고 삭제되지 않은 상품은 true를 반환한다")
        fun isActive_soldOut_returnsTrue() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 0)

            // assert
            assertThat(product.isActive()).isTrue()
        }

        @Test
        @DisplayName("HIDDEN 상태 상품은 false를 반환한다")
        fun isActive_hidden_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct()
            product.update(null, null, null, Product.ProductStatus.HIDDEN)

            // assert
            assertThat(product.isActive()).isFalse()
        }

        @Test
        @DisplayName("삭제된 상품은 false를 반환한다")
        fun isActive_deleted_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct()
            product.delete()

            // assert
            assertThat(product.isActive()).isFalse()
        }

        @Test
        @DisplayName("삭제되고 HIDDEN인 상품은 false를 반환한다")
        fun isActive_deletedAndHidden_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct()
            product.update(null, null, null, Product.ProductStatus.HIDDEN)
            product.delete()

            // assert
            assertThat(product.isActive()).isFalse()
        }
    }

    @Nested
    @DisplayName("isAvailableForOrder 확인 시")
    inner class IsAvailableForOrderTest {

        @Test
        @DisplayName("ON_SALE 상태이고 삭제되지 않은 상품은 true를 반환한다")
        fun isAvailableForOrder_onSale_returnsTrue() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // assert
            assertThat(product.isAvailableForOrder()).isTrue()
        }

        @Test
        @DisplayName("SOLD_OUT 상태 상품은 false를 반환한다")
        fun isAvailableForOrder_soldOut_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct(stock = 0)

            // assert
            assertThat(product.isAvailableForOrder()).isFalse()
        }

        @Test
        @DisplayName("HIDDEN 상태 상품은 false를 반환한다")
        fun isAvailableForOrder_hidden_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct()
            product.update(null, null, null, Product.ProductStatus.HIDDEN)

            // assert
            assertThat(product.isAvailableForOrder()).isFalse()
        }

        @Test
        @DisplayName("삭제된 상품은 false를 반환한다")
        fun isAvailableForOrder_deleted_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct()
            product.delete()

            // assert
            assertThat(product.isAvailableForOrder()).isFalse()
        }
    }

    @Nested
    @DisplayName("isDeleted 확인 시")
    inner class IsDeletedTest {

        @Test
        @DisplayName("삭제되지 않은 상품은 false를 반환한다")
        fun isDeleted_notDeleted_returnsFalse() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // assert
            assertThat(product.isDeleted()).isFalse()
        }

        @Test
        @DisplayName("삭제된 상품은 true를 반환한다")
        fun isDeleted_deleted_returnsTrue() {
            // arrange
            val product = ProductTestFixture.createProduct()

            // act
            product.delete()

            // assert
            assertThat(product.isDeleted()).isTrue()
        }
    }
}
