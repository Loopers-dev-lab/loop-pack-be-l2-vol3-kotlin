package com.loopers.domain.product

import com.loopers.domain.common.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

@DisplayName("ProductStock 도메인")
class ProductStockTest {

    @Nested
    @DisplayName("재고 생성")
    inner class WhenCreate {
        @Test
        @DisplayName("productId와 초기 수량으로 생성하면 id=null로 생성된다")
        fun create_success() {
            val stock = ProductStock.create(productId = 1L, initialQuantity = Quantity(10))

            assertAll(
                { assertThat(stock.id).isNull() },
                { assertThat(stock.productId).isEqualTo(1L) },
                { assertThat(stock.quantity).isEqualTo(Quantity(10)) },
            )
        }
    }

    @Nested
    @DisplayName("재고 차감")
    inner class WhenDecrease {
        @Test
        @DisplayName("재고 10개에서 3개 차감하면 7개가 된다")
        fun decrease_normalCase() {
            val stock = ProductStock.create(productId = 1L, initialQuantity = Quantity(10))
            val decreased = stock.decrease(Quantity(3))
            assertThat(decreased.quantity).isEqualTo(Quantity(7))
        }

        @Test
        @DisplayName("재고 10개에서 10개 차감하면 0개가 된다 (경계값)")
        fun decrease_exactAmount() {
            val stock = ProductStock.create(productId = 1L, initialQuantity = Quantity(10))
            val decreased = stock.decrease(Quantity(10))
            assertThat(decreased.quantity).isEqualTo(Quantity(0))
        }

        @Test
        @DisplayName("재고보다 많이 차감하면 예외를 던진다")
        fun decrease_insufficient() {
            val stock = ProductStock.create(productId = 1L, initialQuantity = Quantity(2))
            val exception = assertThrows<CoreException> { stock.decrease(Quantity(3)) }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_QUANTITY)
        }
    }

    @Nested
    @DisplayName("재고 증가")
    inner class WhenIncrease {
        @Test
        @DisplayName("재고 5개에서 3개 증가하면 8개가 된다")
        fun increase_normalCase() {
            val stock = ProductStock.create(productId = 1L, initialQuantity = Quantity(5))
            val increased = stock.increase(Quantity(3))
            assertThat(increased.quantity).isEqualTo(Quantity(8))
        }
    }
}
