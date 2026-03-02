package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("Money VO")
class MoneyTest {

    @Nested
    @DisplayName("유효한 금액이면 생성에 성공한다")
    inner class WhenValidAmount {
        @Test
        @DisplayName("양수 금액으로 생성한다")
        fun create_positiveAmount() {
            val money = Money(BigDecimal("1000"))
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("1000.00"))
        }

        @Test
        @DisplayName("0원으로 생성한다 (경계값)")
        fun create_zeroAmount() {
            val money = Money(BigDecimal.ZERO)
            assertThat(money.amount).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        @DisplayName("scale 정규화로 동치성을 보장한다 (1000 == 1000.00)")
        fun create_scaleNormalization() {
            val money1 = Money(BigDecimal("1000"))
            val money2 = Money(BigDecimal("1000.00"))
            assertThat(money1).isEqualTo(money2)
        }
    }

    @Nested
    @DisplayName("유효하지 않은 금액이면 생성에 실패한다")
    inner class WhenInvalidAmount {
        @Test
        @DisplayName("음수 금액이면 예외를 던진다")
        fun create_negativeAmount() {
            val exception = assertThrows<CoreException> { Money(BigDecimal("-1")) }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_MONEY)
        }
    }

    @Nested
    @DisplayName("multiply 연산")
    inner class Multiply {
        @Test
        @DisplayName("금액 × 수량 = 총액")
        fun multiply_quantity() {
            val money = Money(BigDecimal("1000"))
            val quantity = Quantity(3)
            val result = money.multiply(quantity)
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("3000.00"))
        }
    }

    @Nested
    @DisplayName("add 연산")
    inner class Add {
        @Test
        @DisplayName("금액 + 금액")
        fun add_money() {
            val money1 = Money(BigDecimal("1000"))
            val money2 = Money(BigDecimal("2500.50"))
            val result = money1.add(money2)
            assertThat(result.amount).isEqualByComparingTo(BigDecimal("3500.50"))
        }
    }

    @Nested
    @DisplayName("isGreaterThan 비교")
    inner class IsGreaterThan {
        @Test
        @DisplayName("금액이 크면 true")
        fun isGreaterThan_greater() {
            val money1 = Money(BigDecimal("2000"))
            val money2 = Money(BigDecimal("1000"))
            assertThat(money1.isGreaterThan(money2)).isTrue()
        }

        @Test
        @DisplayName("금액이 같으면 false")
        fun isGreaterThan_equal() {
            val money1 = Money(BigDecimal("1000"))
            val money2 = Money(BigDecimal("1000"))
            assertThat(money1.isGreaterThan(money2)).isFalse()
        }

        @Test
        @DisplayName("금액이 작으면 false")
        fun isGreaterThan_less() {
            val money1 = Money(BigDecimal("500"))
            val money2 = Money(BigDecimal("1000"))
            assertThat(money1.isGreaterThan(money2)).isFalse()
        }
    }
}
