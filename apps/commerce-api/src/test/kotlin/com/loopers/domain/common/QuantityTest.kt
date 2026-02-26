package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QuantityTest {

    @DisplayName("Quantity 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("값이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenValueIsZeroOrNegative() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                Quantity.of(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("양수 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsQuantity_whenPositiveValueProvided() {
            // arrange & act
            val quantity = Quantity.of(5)

            // assert
            assertThat(quantity.value).isEqualTo(5)
        }
    }

    @DisplayName("값 동등성을 비교할 때,")
    @Nested
    inner class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        fun isEqual_whenSameValue() {
            // arrange
            val quantity1 = Quantity.of(3)
            val quantity2 = Quantity.of(3)

            // assert
            assertThat(quantity1).isEqualTo(quantity2)
            assertThat(quantity1.hashCode()).isEqualTo(quantity2.hashCode())
        }
    }

    @DisplayName("toString을 호출하면,")
    @Nested
    inner class ToString {

        @DisplayName("value 값을 문자열로 반환한다.")
        @Test
        fun returnsValueAsString() {
            // arrange
            val quantity = Quantity.of(7)

            // act & assert
            assertThat(quantity.toString()).isEqualTo("7")
        }
    }
}
