package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Quantity VO")
class QuantityTest {

    @Nested
    @DisplayName("유효한 수량이면 생성에 성공한다")
    inner class WhenValidValue {
        @Test
        @DisplayName("양수 수량으로 생성한다")
        fun create_positiveValue() {
            val quantity = Quantity(10)
            assertThat(quantity.value).isEqualTo(10)
        }

        @Test
        @DisplayName("0으로 생성한다 (경계값)")
        fun create_zeroValue() {
            val quantity = Quantity(0)
            assertThat(quantity.value).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("유효하지 않은 수량이면 생성에 실패한다")
    inner class WhenInvalidValue {
        @Test
        @DisplayName("음수이면 예외를 던진다")
        fun create_negativeValue() {
            val exception = assertThrows<CoreException> { Quantity(-1) }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_QUANTITY)
        }
    }

    @Nested
    @DisplayName("decrease 연산")
    inner class Decrease {
        @Test
        @DisplayName("정상 차감")
        fun decrease_normal() {
            val quantity = Quantity(10)
            val result = quantity.decrease(Quantity(3))
            assertThat(result.value).isEqualTo(7)
        }

        @Test
        @DisplayName("동일값 차감 → 0 (경계값)")
        fun decrease_toZero() {
            val quantity = Quantity(5)
            val result = quantity.decrease(Quantity(5))
            assertThat(result.value).isEqualTo(0)
        }

        @Test
        @DisplayName("부족 시 예외를 던진다")
        fun decrease_insufficient() {
            val quantity = Quantity(2)
            val exception = assertThrows<CoreException> { quantity.decrease(Quantity(3)) }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_QUANTITY)
        }
    }

    @Nested
    @DisplayName("increase 연산")
    inner class Increase {
        @Test
        @DisplayName("정상 증가")
        fun increase_normal() {
            val quantity = Quantity(5)
            val result = quantity.increase(Quantity(3))
            assertThat(result.value).isEqualTo(8)
        }
    }

    @Nested
    @DisplayName("isEnoughFor 검증")
    inner class IsEnoughFor {
        @Test
        @DisplayName("충분하면 true")
        fun isEnoughFor_enough() {
            val stock = Quantity(10)
            assertThat(stock.isEnoughFor(Quantity(5))).isTrue()
        }

        @Test
        @DisplayName("동일값이면 true (경계값)")
        fun isEnoughFor_exact() {
            val stock = Quantity(10)
            assertThat(stock.isEnoughFor(Quantity(10))).isTrue()
        }

        @Test
        @DisplayName("부족하면 false")
        fun isEnoughFor_insufficient() {
            val stock = Quantity(2)
            assertThat(stock.isEnoughFor(Quantity(3))).isFalse()
        }
    }
}
