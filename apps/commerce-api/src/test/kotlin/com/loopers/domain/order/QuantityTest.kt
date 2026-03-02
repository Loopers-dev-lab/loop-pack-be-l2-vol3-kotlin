package com.loopers.domain.order

import com.loopers.domain.common.vo.Quantity
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
        @DisplayName("1 이상이면 정상 생성된다")
        fun create_withPositiveValue_success() {
            // arrange & act
            val quantity = Quantity(5)

            // assert
            assertThat(quantity.value).isEqualTo(5)
        }

        @Test
        @DisplayName("0이면 BAD_REQUEST 예외가 발생한다")
        fun create_withZero_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { Quantity(0) }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("수량은 1 이상이어야 합니다.")
        }

        @Test
        @DisplayName("음수이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNegativeValue_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { Quantity(-1) }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("수량은 1 이상이어야 합니다.")
        }
    }
}
