package com.loopers.domain.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MoneyTest {

    @DisplayName("Money 연산할 때,")
    @Nested
    inner class Operation {

        @DisplayName("Quantity를 곱하면, 금액 * 수량 결과를 반환한다.")
        @Test
        fun returnsMultipliedMoney_whenMultipliedByQuantity() {
            // arrange
            val money = Money.of(1000)
            val quantity = Quantity.of(3)

            // act
            val result = money * quantity

            // assert
            assertThat(result).isEqualTo(Money.of(3000))
        }
    }
}
