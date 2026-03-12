package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductStockTest {

    @DisplayName("재고 차감")
    @Nested
    inner class DecreaseStock {

        @DisplayName("재고가 충분하면 차감에 성공한다")
        @Test
        fun success() {
            val productStock = ProductStock.create(productId = 1L, stock = Stock(10))

            productStock.decreaseStock(3)

            assertThat(productStock.stock).isEqualTo(Stock(7))
        }

        @DisplayName("재고가 부족하면 INSUFFICIENT_STOCK 예외가 발생한다")
        @Test
        fun failWhenInsufficient() {
            val productStock = ProductStock.create(productId = 1L, stock = Stock(3))

            val exception = assertThrows<CoreException> {
                productStock.decreaseStock(5)
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INSUFFICIENT_STOCK)
        }
    }

    @DisplayName("재고 수정")
    @Nested
    inner class UpdateStock {

        @DisplayName("재고를 수정할 수 있다")
        @Test
        fun success() {
            val productStock = ProductStock.create(productId = 1L, stock = Stock(10))

            productStock.updateStock(Stock(50))

            assertThat(productStock.stock).isEqualTo(Stock(50))
        }
    }
}
