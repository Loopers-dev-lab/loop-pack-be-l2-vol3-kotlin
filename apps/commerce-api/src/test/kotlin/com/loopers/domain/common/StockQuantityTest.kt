package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StockQuantityTest {

    @DisplayName("StockQuantity 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("값이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenValueIsNegative() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                StockQuantity.of(-1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("0이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsStockQuantity_whenZeroProvided() {
            // arrange & act
            val stockQuantity = StockQuantity.of(0)

            // assert
            assertThat(stockQuantity.value).isEqualTo(0)
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class Minus {

        @DisplayName("차감 결과가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenResultIsNegative() {
            // arrange
            val stockQuantity = StockQuantity.of(5)
            val deductQuantity = Quantity.of(6)

            // act & assert
            val exception = assertThrows<CoreException> {
                stockQuantity - deductQuantity
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("차감 결과가 0 이상이면, 차감된 StockQuantity를 반환한다.")
        @Test
        fun returnsDeductedStockQuantity_whenResultIsNonNegative() {
            // arrange
            val stockQuantity = StockQuantity.of(10)
            val deductQuantity = Quantity.of(3)

            // act
            val result = stockQuantity - deductQuantity

            // assert
            assertThat(result.value).isEqualTo(7)
        }
    }

    @DisplayName("값 동등성을 비교할 때,")
    @Nested
    inner class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        fun isEqual_whenSameValue() {
            // arrange
            val sq1 = StockQuantity.of(10)
            val sq2 = StockQuantity.of(10)

            // assert
            assertThat(sq1).isEqualTo(sq2)
            assertThat(sq1.hashCode()).isEqualTo(sq2.hashCode())
        }
    }
}
