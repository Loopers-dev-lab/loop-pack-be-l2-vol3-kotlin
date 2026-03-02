package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QuantityTest {

    @DisplayName("Quantity 생성")
    @Nested
    inner class Create {

        @DisplayName("1 이상이면 생성할 수 있다")
        @Test
        fun successWithPositiveValue() {
            val quantity = Quantity(value = 5)

            assertThat(quantity.value).isEqualTo(5)
        }

        @DisplayName("1이면 생성할 수 있다")
        @Test
        fun successWithOne() {
            val quantity = Quantity(value = 1)

            assertThat(quantity.value).isEqualTo(1)
        }

        @DisplayName("0이면 INVALID_QUANTITY 예외가 발생한다")
        @Test
        fun failWhenZero() {
            val exception = assertThrows<CoreException> {
                Quantity(value = 0)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.INVALID_QUANTITY)
        }

        @DisplayName("음수이면 INVALID_QUANTITY 예외가 발생한다")
        @Test
        fun failWhenNegative() {
            val exception = assertThrows<CoreException> {
                Quantity(value = -1)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.INVALID_QUANTITY)
        }
    }

    @DisplayName("Quantity 값 비교")
    @Nested
    inner class Equality {

        @DisplayName("같은 값이면 동등하다")
        @Test
        fun equalWhenSameValue() {
            val q1 = Quantity(value = 3)
            val q2 = Quantity(value = 3)

            assertThat(q1).isEqualTo(q2)
        }
    }
}
