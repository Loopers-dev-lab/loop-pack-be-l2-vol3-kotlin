package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StockTest {

    @Nested
    @DisplayName("Stock 생성 시")
    inner class Create {

        @Test
        @DisplayName("0 이상이면 정상 생성된다")
        fun create_withValidStock_success() {
            // arrange & act
            val stock = Stock(100)

            // assert
            assertThat(stock.value).isEqualTo(100)
        }

        @Test
        @DisplayName("0이면 정상 생성된다")
        fun create_withZero_success() {
            // arrange & act
            val stock = Stock(0)

            // assert
            assertThat(stock.value).isEqualTo(0)
        }

        @Test
        @DisplayName("음수이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNegativeStock_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { Stock(-1) }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("재고는 0 이상이어야 합니다.")
        }
    }

    @Nested
    @DisplayName("decrease 시")
    inner class Decrease {

        @Test
        @DisplayName("충분한 재고가 있으면 감소된다")
        fun decrease_withSufficientStock_success() {
            // arrange
            val stock = Stock(10)

            // act
            val decreased = stock.decrease(3)

            // assert
            assertThat(decreased.value).isEqualTo(7)
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        fun decrease_withInsufficientStock_throwsException() {
            // arrange
            val stock = Stock(2)

            // act & assert
            val exception = assertThrows<CoreException> { stock.decrease(3) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("재고가 부족합니다.")
        }

        @Test
        @DisplayName("전량 차감하면 0이 된다")
        fun decrease_allStock_returnsZero() {
            // arrange
            val stock = Stock(5)

            // act
            val decreased = stock.decrease(5)

            // assert
            assertThat(decreased.value).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("increase 시")
    inner class Increase {

        @Test
        @DisplayName("재고가 증가된다")
        fun increase_stock_success() {
            // arrange
            val stock = Stock(10)

            // act
            val increased = stock.increase(5)

            // assert
            assertThat(increased.value).isEqualTo(15)
        }
    }
}
