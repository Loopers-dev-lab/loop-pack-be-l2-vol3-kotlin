package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StockTest {

    @DisplayName("Stock 생성")
    @Nested
    inner class Create {

        @DisplayName("양수 수량으로 생성할 수 있다")
        @Test
        fun successWithPositiveQuantity() {
            val stock = Stock(quantity = 100)

            assertThat(stock.quantity).isEqualTo(100)
        }

        @DisplayName("0으로 생성할 수 있다")
        @Test
        fun successWithZero() {
            val stock = Stock(quantity = 0)

            assertThat(stock.quantity).isEqualTo(0)
        }

        @DisplayName("음수 수량이면 INVALID_STOCK 예외가 발생한다")
        @Test
        fun failWhenNegative() {
            val exception = assertThrows<CoreException> {
                Stock(quantity = -1)
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_STOCK)
        }
    }

    @DisplayName("Stock 차감")
    @Nested
    inner class Deduct {

        @DisplayName("충분한 재고가 있으면 차감에 성공한다")
        @Test
        fun successWhenSufficient() {
            val stock = Stock(quantity = 10)

            val deducted = stock.deduct(3)

            assertThat(deducted.quantity).isEqualTo(7)
        }

        @DisplayName("전량 차감할 수 있다")
        @Test
        fun successWhenExactAmount() {
            val stock = Stock(quantity = 5)

            val deducted = stock.deduct(5)

            assertThat(deducted.quantity).isEqualTo(0)
        }

        @DisplayName("재고가 부족하면 INSUFFICIENT_STOCK 예외가 발생한다")
        @Test
        fun failWhenInsufficient() {
            val stock = Stock(quantity = 3)

            val exception = assertThrows<CoreException> {
                stock.deduct(5)
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INSUFFICIENT_STOCK)
        }

        @DisplayName("차감 후 원본 Stock은 변경되지 않는다")
        @Test
        fun originalNotModified() {
            val original = Stock(quantity = 10)

            original.deduct(3)

            assertThat(original.quantity).isEqualTo(10)
        }
    }

    @DisplayName("Stock 값 비교")
    @Nested
    inner class Equality {

        @DisplayName("같은 수량이면 동등하다")
        @Test
        fun equalWhenSameQuantity() {
            val stock1 = Stock(quantity = 10)
            val stock2 = Stock(quantity = 10)

            assertThat(stock1).isEqualTo(stock2)
        }
    }
}
