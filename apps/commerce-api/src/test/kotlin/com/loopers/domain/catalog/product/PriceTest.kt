package com.loopers.domain.catalog.product

import com.loopers.domain.catalog.product.vo.Price

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PriceTest {

    @Nested
    @DisplayName("Price 생성 시")
    inner class Create {

        @Test
        @DisplayName("0 이상이면 정상 생성된다")
        fun create_withValidPrice_success() {
            // arrange & act
            val price = Price(BigDecimal("10000"))

            // assert
            assertThat(price.value).isEqualByComparingTo(BigDecimal("10000"))
        }

        @Test
        @DisplayName("0이면 정상 생성된다")
        fun create_withZero_success() {
            // arrange & act
            val price = Price(BigDecimal.ZERO)

            // assert
            assertThat(price.value).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        @DisplayName("음수이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNegativePrice_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { Price(BigDecimal("-1")) }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("가격은 0 이상이어야 합니다.")
        }
    }
}
