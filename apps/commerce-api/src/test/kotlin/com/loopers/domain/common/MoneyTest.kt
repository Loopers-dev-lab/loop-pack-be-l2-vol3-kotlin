package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Money VO")
class MoneyTest {

    @DisplayName("유효한 금액이 주어지면, 정상적으로 생성된다.")
    @Test
    fun createsMoney_whenValidValueIsProvided() {
        // arrange
        val expectedValue = BigDecimal("10000")

        // act
        val money = Money.of(expectedValue)

        // assert
        assertThat(money.value).isEqualByComparingTo(expectedValue)
    }

    @DisplayName("검증")
    @Nested
    inner class Validation {

        @DisplayName("금액이 0일 때, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenValueIsZero() {
            // act
            val result = assertThrows<CoreException> {
                Money.of(BigDecimal.ZERO)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("금액이 음수일 때, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenValueIsNegative() {
            // act
            val result = assertThrows<CoreException> {
                Money.of(BigDecimal("-1000"))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
