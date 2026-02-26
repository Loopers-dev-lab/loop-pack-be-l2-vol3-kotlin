package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MoneyTest {

    @DisplayName("Money 생성")
    @Nested
    inner class Create {

        @DisplayName("양수 금액으로 생성할 수 있다")
        @Test
        fun successWithPositiveAmount() {
            val money = Money(amount = 10000)

            assertThat(money.amount).isEqualTo(10000)
        }

        @DisplayName("0원으로 생성할 수 있다")
        @Test
        fun successWithZero() {
            val money = Money(amount = 0)

            assertThat(money.amount).isEqualTo(0)
        }

        @DisplayName("음수 금액이면 INVALID_PRICE 예외가 발생한다")
        @Test
        fun failWhenNegative() {
            val exception = assertThrows<CoreException> {
                Money(amount = -1)
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_PRICE)
        }
    }

    @DisplayName("Money 값 비교")
    @Nested
    inner class Equality {

        @DisplayName("같은 금액이면 동등하다")
        @Test
        fun equalWhenSameAmount() {
            val money1 = Money(amount = 5000)
            val money2 = Money(amount = 5000)

            assertThat(money1).isEqualTo(money2)
        }
    }
}
