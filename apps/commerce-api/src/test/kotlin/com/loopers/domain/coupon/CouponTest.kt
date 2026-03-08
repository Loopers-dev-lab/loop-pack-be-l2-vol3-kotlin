package com.loopers.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("Coupon")
class CouponTest {

    @DisplayName("쿠폰")
    @Nested
    inner class CouponEntity {

        @DisplayName("쿠폰을 발급한다")
        @Test
        fun issueCoupon_success() {
            // arrange
            val userId = 1L
            val template = CouponTemplate.create(
                name = "신규 가입 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val coupon = Coupon.issue(userId, template)

            // assert
            assertThat(coupon.userId).isEqualTo(userId)
            assertThat(coupon.templateId).isEqualTo(template.id)
            assertThat(coupon.status).isEqualTo(CouponStatus.ISSUED)
            assertThat(coupon.usedAt).isNull()
        }

        @DisplayName("쿠폰이 아직 유효한지 확인한다")
        @Test
        fun isValid_returnsTrue_whenCouponIsValid() {
            // arrange
            val userId = 1L
            val template = CouponTemplate.create(
                name = "신규 가입 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val coupon = Coupon.issue(userId, template)

            // act
            val isValid = coupon.isValid()

            // assert
            assertThat(isValid).isTrue()
        }
    }
}
