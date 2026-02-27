package com.loopers.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MoneyTest {

    @Nested
    inner class Create {

        @Test
        @DisplayName("양수 금액으로 생성하면 성공한다")
        fun positiveAmountSuccess() {
            // arrange & act
            val money = Money(1000)

            // assert
            assertThat(money.amount).isEqualTo(1000)
        }

        @Test
        @DisplayName("0원으로 생성하면 성공한다")
        fun zeroAmountSuccess() {
            // arrange & act
            val money = Money(0)

            // assert
            assertThat(money.amount).isEqualTo(0)
        }

        @Test
        @DisplayName("음수 금액이면 IllegalArgumentException이 발생한다")
        fun negativeAmountThrowsException() {
            // act & assert
            assertThrows<IllegalArgumentException> {
                Money(-1)
            }
        }

        @Test
        @DisplayName("ZERO 상수는 0원이다")
        fun zeroConstant() {
            // assert
            assertThat(Money.ZERO.amount).isEqualTo(0)
        }
    }

    @Nested
    inner class Arithmetic {

        @Test
        @DisplayName("두 금액을 더하면 합계가 반환된다")
        fun plusSuccess() {
            // arrange
            val a = Money(1000)
            val b = Money(2000)

            // act
            val result = a + b

            // assert
            assertThat(result).isEqualTo(Money(3000))
        }

        @Test
        @DisplayName("큰 금액에서 작은 금액을 빼면 차액이 반환된다")
        fun minusSuccess() {
            // arrange
            val a = Money(3000)
            val b = Money(1000)

            // act
            val result = a - b

            // assert
            assertThat(result).isEqualTo(Money(2000))
        }

        @Test
        @DisplayName("빼기 결과가 음수이면 IllegalArgumentException이 발생한다")
        fun minusNegativeThrowsException() {
            // arrange
            val a = Money(1000)
            val b = Money(2000)

            // act & assert
            assertThrows<IllegalArgumentException> {
                a - b
            }
        }

        @Test
        @DisplayName("금액에 수량을 곱하면 총액이 반환된다")
        fun timesSuccess() {
            // arrange
            val price = Money(15000)

            // act
            val result = price * 3

            // assert
            assertThat(result).isEqualTo(Money(45000))
        }
    }

    @Nested
    inner class Compare {

        @Test
        @DisplayName("큰 금액이 작은 금액보다 크다")
        fun greaterThan() {
            // arrange
            val a = Money(2000)
            val b = Money(1000)

            // assert
            assertThat(a > b).isTrue()
        }

        @Test
        @DisplayName("같은 금액은 동등하다")
        fun equal() {
            // arrange
            val a = Money(1000)
            val b = Money(1000)

            // assert
            assertThat(a).isEqualTo(b)
            assertThat(a.compareTo(b)).isEqualTo(0)
        }
    }
}
