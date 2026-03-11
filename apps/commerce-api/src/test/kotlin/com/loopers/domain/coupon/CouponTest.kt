package com.loopers.domain.coupon

import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.MinOrderAmount
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTest {

    @Nested
    inner class Create {
        @Test
        fun `RATE_타입에서_할인값이_100을_초과하면_예외가_발생한다`() {
            val result = assertThrows<CoreException> {
                createCoupon(type = CouponType.RATE, discountValue = 101L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_COUPON_VALUE)
        }

        @Test
        fun `RATE_타입에서_할인값이_100이면_생성할_수_있다`() {
            val coupon = createCoupon(type = CouponType.RATE, discountValue = 100L)
            assertThat(coupon.discountValue.value).isEqualTo(100L)
        }

        @Test
        fun `FIXED_타입에서_할인값이_100을_초과해도_생성할_수_있다`() {
            val coupon = createCoupon(type = CouponType.FIXED, discountValue = 50000L)
            assertThat(coupon.discountValue.value).isEqualTo(50000L)
        }
    }

    @Nested
    inner class CalculateDiscount {
        @Test
        fun `FIXED_타입은_할인금액_그대로_반환한다`() {
            val coupon = createCoupon(type = CouponType.FIXED, discountValue = 3000L)
            val discount = coupon.calculateDiscount(10000L)
            assertThat(discount).isEqualTo(3000L)
        }

        @Test
        fun `FIXED_타입에서_할인금액이_원가보다_크면_원가를_반환한다`() {
            val coupon = createCoupon(type = CouponType.FIXED, discountValue = 15000L)
            val discount = coupon.calculateDiscount(10000L)
            assertThat(discount).isEqualTo(10000L)
        }

        @Test
        fun `RATE_타입은_비율에_따라_할인금액을_반환한다`() {
            val coupon = createCoupon(type = CouponType.RATE, discountValue = 10L)
            val discount = coupon.calculateDiscount(10000L)
            assertThat(discount).isEqualTo(1000L)
        }

        @Test
        fun `RATE_50퍼센트_할인을_정확히_계산한다`() {
            val coupon = createCoupon(type = CouponType.RATE, discountValue = 50L)
            val discount = coupon.calculateDiscount(10000L)
            assertThat(discount).isEqualTo(5000L)
        }
    }

    @Nested
    inner class IsExpired {
        @Test
        fun `만료일이_지나지_않으면_false를_반환한다`() {
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().plusDays(1))
            assertThat(coupon.isExpired()).isFalse()
        }

        @Test
        fun `만료일이_지나면_true를_반환한다`() {
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            assertThat(coupon.isExpired()).isTrue()
        }
    }

    @Nested
    inner class ValidateIssuable {
        @Test
        fun `만료되지_않은_쿠폰은_발급_가능하다`() {
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().plusDays(1))
            coupon.validateIssuable()
        }

        @Test
        fun `만료된_쿠폰은_발급_불가하다`() {
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            val result = assertThrows<CoreException> { coupon.validateIssuable() }
            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_EXPIRED)
        }
    }

    private fun createCoupon(
        type: CouponType = CouponType.FIXED,
        discountValue: Long = 1000L,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return Coupon(
            name = CouponName("테스트쿠폰"),
            type = type,
            discountValue = DiscountValue(discountValue),
            minOrderAmount = MinOrderAmount(null),
            expiredAt = expiredAt,
        )
    }
}
