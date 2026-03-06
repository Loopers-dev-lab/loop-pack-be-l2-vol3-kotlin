package com.loopers.domain.coupon

import com.loopers.domain.product.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTest {

    private val validExpiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30)

    @DisplayName("쿠폰 생성")
    @Nested
    inner class Create {

        @DisplayName("정액 할인 쿠폰을 정상 생성한다")
        @Test
        fun successFixedCoupon() {
            val coupon = Coupon.create(
                name = "1000원 할인",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = 10000,
                expiredAt = validExpiredAt,
            )

            assertAll(
                { assertThat(coupon.name).isEqualTo("1000원 할인") },
                { assertThat(coupon.type).isEqualTo(CouponType.FIXED) },
                { assertThat(coupon.value).isEqualTo(1000) },
                { assertThat(coupon.minOrderAmount).isEqualTo(10000) },
            )
        }

        @DisplayName("정률 할인 쿠폰을 정상 생성한다")
        @Test
        fun successRateCoupon() {
            val coupon = Coupon.create(
                name = "10% 할인",
                type = CouponType.RATE,
                value = 10,
                minOrderAmount = null,
                expiredAt = validExpiredAt,
            )

            assertAll(
                { assertThat(coupon.type).isEqualTo(CouponType.RATE) },
                { assertThat(coupon.value).isEqualTo(10) },
                { assertThat(coupon.minOrderAmount).isNull() },
            )
        }

        @DisplayName("쿠폰명이 비어있으면 INVALID_COUPON_NAME 에러가 발생한다")
        @Test
        fun failWhenEmptyName() {
            val exception = assertThrows<CoreException> {
                Coupon.create(name = "  ", type = CouponType.FIXED, value = 1000, minOrderAmount = null, expiredAt = validExpiredAt)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.INVALID_COUPON_NAME)
        }

        @DisplayName("할인 값이 0이면 INVALID_COUPON_VALUE 에러가 발생한다")
        @Test
        fun failWhenZeroValue() {
            val exception = assertThrows<CoreException> {
                Coupon.create(name = "쿠폰", type = CouponType.FIXED, value = 0, minOrderAmount = null, expiredAt = validExpiredAt)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.INVALID_COUPON_VALUE)
        }

        @DisplayName("정률 할인이 100을 초과하면 INVALID_RATE_VALUE 에러가 발생한다")
        @Test
        fun failWhenRateExceeds100() {
            val exception = assertThrows<CoreException> {
                Coupon.create(name = "쿠폰", type = CouponType.RATE, value = 101, minOrderAmount = null, expiredAt = validExpiredAt)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.INVALID_RATE_VALUE)
        }

        @DisplayName("최소 주문 금액이 음수이면 INVALID_MIN_ORDER_AMOUNT 에러가 발생한다")
        @Test
        fun failWhenNegativeMinOrderAmount() {
            val exception = assertThrows<CoreException> {
                Coupon.create(name = "쿠폰", type = CouponType.FIXED, value = 1000, minOrderAmount = -1, expiredAt = validExpiredAt)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.INVALID_MIN_ORDER_AMOUNT)
        }
    }

    @DisplayName("할인 금액 계산")
    @Nested
    inner class CalculateDiscount {

        @DisplayName("정액 쿠폰은 할인값만큼 할인된다")
        @Test
        fun fixedDiscount() {
            val coupon = Coupon.create(
                name = "3000원 할인", type = CouponType.FIXED, value = 3000,
                minOrderAmount = null, expiredAt = validExpiredAt,
            )

            val discount = coupon.calculateDiscount(Money(10000))

            assertThat(discount).isEqualTo(Money(3000))
        }

        @DisplayName("정액 쿠폰 할인값이 주문금액보다 크면 주문금액만큼 할인된다")
        @Test
        fun fixedDiscountCappedByOrderAmount() {
            val coupon = Coupon.create(
                name = "5000원 할인", type = CouponType.FIXED, value = 5000,
                minOrderAmount = null, expiredAt = validExpiredAt,
            )

            val discount = coupon.calculateDiscount(Money(3000))

            assertThat(discount).isEqualTo(Money(3000))
        }

        @DisplayName("정률 쿠폰은 주문금액의 비율만큼 할인된다")
        @Test
        fun rateDiscount() {
            val coupon = Coupon.create(
                name = "10% 할인", type = CouponType.RATE, value = 10,
                minOrderAmount = null, expiredAt = validExpiredAt,
            )

            val discount = coupon.calculateDiscount(Money(10000))

            assertThat(discount).isEqualTo(Money(1000))
        }

        @DisplayName("정률 쿠폰 소수점은 버림 처리된다")
        @Test
        fun rateDiscountTruncated() {
            val coupon = Coupon.create(
                name = "15% 할인", type = CouponType.RATE, value = 15,
                minOrderAmount = null, expiredAt = validExpiredAt,
            )

            val discount = coupon.calculateDiscount(Money(9999))

            // 9999 * 15 / 100 = 1499.85 → 1499 (Long 나눗셈은 자동 버림)
            assertThat(discount).isEqualTo(Money(1499))
        }
    }

    @DisplayName("적용 가능 검증")
    @Nested
    inner class ValidateApplicable {

        @DisplayName("만료된 쿠폰이면 COUPON_EXPIRED 에러가 발생한다")
        @Test
        fun failWhenExpired() {
            val coupon = Coupon.create(
                name = "만료 쿠폰", type = CouponType.FIXED, value = 1000,
                minOrderAmount = null, expiredAt = ZonedDateTime.now().minusDays(1),
            )

            val exception = assertThrows<CoreException> {
                coupon.validateApplicable(Money(10000))
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_EXPIRED)
        }

        @DisplayName("최소 주문 금액 미달이면 MIN_ORDER_AMOUNT_NOT_MET 에러가 발생한다")
        @Test
        fun failWhenMinOrderAmountNotMet() {
            val coupon = Coupon.create(
                name = "쿠폰", type = CouponType.FIXED, value = 1000,
                minOrderAmount = 10000, expiredAt = validExpiredAt,
            )

            val exception = assertThrows<CoreException> {
                coupon.validateApplicable(Money(5000))
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET)
        }

        @DisplayName("최소 주문 금액이 없으면 금액과 무관하게 통과한다")
        @Test
        fun successWhenNoMinOrderAmount() {
            val coupon = Coupon.create(
                name = "쿠폰", type = CouponType.FIXED, value = 1000,
                minOrderAmount = null, expiredAt = validExpiredAt,
            )

            coupon.validateApplicable(Money(100))
        }
    }

    @DisplayName("쿠폰 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 deletedAt이 설정된다")
        @Test
        fun success() {
            val coupon = Coupon.create(
                name = "쿠폰", type = CouponType.FIXED, value = 1000,
                minOrderAmount = null, expiredAt = validExpiredAt,
            )

            coupon.delete()

            assertThat(coupon.isDeleted()).isTrue()
        }
    }
}
