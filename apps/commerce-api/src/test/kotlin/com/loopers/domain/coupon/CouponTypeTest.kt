package com.loopers.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("CouponType")
class CouponTypeTest {

    @DisplayName("고정 금액 할인의 할인액을 계산한다")
    @Test
    fun calculateDiscountAmount_withFixedAmount() {
        // arrange
        val originalPrice = BigDecimal("10000")
        val discountValue = BigDecimal("2000")

        // act
        val discountAmount = CouponType.FIXED.calculateDiscount(originalPrice, discountValue)

        // assert
        assertThat(discountAmount).isEqualTo(BigDecimal("2000"))
    }

    @DisplayName("할인율의 할인액을 계산한다")
    @Test
    fun calculateDiscountAmount_withPercent() {
        // arrange
        val originalPrice = BigDecimal("10000")
        val discountValue = BigDecimal("10") // 10%

        // act
        val discountAmount = CouponType.RATE.calculateDiscount(originalPrice, discountValue)

        // assert
        assertThat(discountAmount).isEqualTo(BigDecimal("1000"))
    }

    @DisplayName("할인율이 100% 이상일 수는 없다")
    @Test
    fun calculateDiscountAmount_withPercentOver100() {
        // arrange
        val originalPrice = BigDecimal("10000")
        val discountValue = BigDecimal("150") // 150%

        // act
        val discountAmount = CouponType.RATE.calculateDiscount(originalPrice, discountValue)

        // assert - 최대 할인액은 원가
        assertThat(discountAmount).isEqualTo(originalPrice)
    }
}
