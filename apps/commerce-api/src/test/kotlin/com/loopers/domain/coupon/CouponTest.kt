package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

        @DisplayName("발급된 쿠폰을 사용한다")
        @Test
        fun useCoupon_success() {
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
            coupon.use()

            // assert
            assertThat(coupon.status).isEqualTo(CouponStatus.USED)
            assertThat(coupon.usedAt).isNotNull()
        }

        @DisplayName("이미 사용된 쿠폰을 다시 사용할 수 없다")
        @Test
        fun useCoupon_throwsException_whenAlreadyUsed() {
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
            coupon.use()

            // act & assert
            assertThatThrownBy { coupon.use() }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
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

        @DisplayName("이미 사용된 쿠폰은 유효하지 않다")
        @Test
        fun isValid_returnsFalse_whenCouponIsAlreadyUsed() {
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
            coupon.use()

            // act
            val isValid = coupon.isValid()

            // assert
            assertThat(isValid).isFalse()
        }

        @DisplayName("이미 사용된 쿠폰은 적용할 수 없다")
        @Test
        fun canApplyToOrder_returnsFalse_whenCouponIsAlreadyUsed() {
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
            coupon.use()
            val orderAmount = BigDecimal("15000")

            // act
            val canApply = coupon.canApplyToOrder(orderAmount)

            // assert
            assertThat(canApply).isFalse()
        }
    }
}
