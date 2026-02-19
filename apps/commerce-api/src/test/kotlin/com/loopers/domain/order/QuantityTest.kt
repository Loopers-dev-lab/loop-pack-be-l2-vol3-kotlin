package com.loopers.domain.order

import com.loopers.domain.order.vo.Quantity

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QuantityTest {

    @Nested
    @DisplayName("Quantity 생성 시")
    inner class Create {

        @Test
        @DisplayName("1 이상의 값으로 생성하면 정상 생성된다")
        fun create_withValidValue_success() {
            // act
            val quantity = Quantity(1)

            // assert
            assertThat(quantity.value).isEqualTo(1)
        }

        @Test
        @DisplayName("큰 수량으로 생성하면 정상 생성된다")
        fun create_withLargeValue_success() {
            // act
            val quantity = Quantity(100)

            // assert
            assertThat(quantity.value).isEqualTo(100)
        }

        @Test
        @DisplayName("0으로 생성하면 CoreException이 발생한다")
        fun create_withZero_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Quantity(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("음수로 생성하면 CoreException이 발생한다")
        fun create_withNegative_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Quantity(-1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("Quantity 동등성 비교 시")
    inner class Equality {

        @Test
        @DisplayName("같은 값이면 동등하다")
        fun equality_sameValue_areEqual() {
            // arrange
            val quantity1 = Quantity(5)
            val quantity2 = Quantity(5)

            // assert
            assertThat(quantity1).isEqualTo(quantity2)
            assertThat(quantity1.hashCode()).isEqualTo(quantity2.hashCode())
        }

        @Test
        @DisplayName("Set에서 동일 값은 중복 제거된다")
        fun equality_inSet_deduplicates() {
            // arrange
            val quantity1 = Quantity(5)
            val quantity2 = Quantity(5)

            // act
            val set = setOf(quantity1, quantity2)

            // assert
            assertThat(set).hasSize(1)
        }
    }
}
