package com.loopers.domain.coupon

import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTest {

    private fun createCoupon(
        name: String = "신규가입 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: Long = 5000,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon = Coupon(
        name = name,
        type = type,
        value = value,
        expiredAt = expiredAt,
    )

    @Nested
    inner class CreateCoupon {

        @Test
        @DisplayName("올바른 정보로 정액 쿠폰을 생성하면 성공한다")
        fun fixedCouponSuccess() {
            // arrange & act
            val coupon = createCoupon(type = CouponType.FIXED, value = 5000)

            // assert
            assertAll(
                { assertThat(coupon.name).isEqualTo("신규가입 쿠폰") },
                { assertThat(coupon.type).isEqualTo(CouponType.FIXED) },
                { assertThat(coupon.value).isEqualTo(5000) },
            )
        }

        @Test
        @DisplayName("올바른 정보로 정률 쿠폰을 생성하면 성공한다")
        fun rateCouponSuccess() {
            // arrange & act
            val coupon = createCoupon(type = CouponType.RATE, value = 10)

            // assert
            assertAll(
                { assertThat(coupon.type).isEqualTo(CouponType.RATE) },
                { assertThat(coupon.value).isEqualTo(10) },
            )
        }

        @Test
        @DisplayName("쿠폰명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCoupon(name = "   ")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("할인 값이 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroValueThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCoupon(value = 0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("할인 값이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeValueThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCoupon(value = -1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("정률 할인이 100%를 초과하면 BAD_REQUEST 예외가 발생한다")
        fun rateOver100ThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCoupon(type = CouponType.RATE, value = 101)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("정률 할인 100%는 허용된다")
        fun rate100IsAllowed() {
            val coupon = createCoupon(type = CouponType.RATE, value = 100)
            assertThat(coupon.value).isEqualTo(100)
        }
    }

    @Nested
    inner class UpdateCoupon {

        @Test
        @DisplayName("올바른 정보로 수정하면 필드가 변경된다")
        fun success() {
            // arrange
            val coupon = createCoupon()
            val newExpiredAt = ZonedDateTime.now().plusDays(60)

            // act
            coupon.update(
                name = "봄맞이 쿠폰",
                type = CouponType.RATE,
                value = 15,
                expiredAt = newExpiredAt,
            )

            // assert
            assertAll(
                { assertThat(coupon.name).isEqualTo("봄맞이 쿠폰") },
                { assertThat(coupon.type).isEqualTo(CouponType.RATE) },
                { assertThat(coupon.value).isEqualTo(15) },
                { assertThat(coupon.expiredAt).isEqualTo(newExpiredAt) },
            )
        }

        @Test
        @DisplayName("수정 시 쿠폰명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            val coupon = createCoupon()

            val result = assertThrows<CoreException> {
                coupon.update(
                    name = "",
                    type = CouponType.FIXED,
                    value = 1000,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("수정 시 정률 할인이 100%를 초과하면 BAD_REQUEST 예외가 발생한다")
        fun rateOver100ThrowsBadRequest() {
            val coupon = createCoupon()

            val result = assertThrows<CoreException> {
                coupon.update(
                    name = "쿠폰",
                    type = CouponType.RATE,
                    value = 101,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class IsExpired {

        @Test
        @DisplayName("만료 일시가 지나면 만료 상태이다")
        fun expiredWhenPast() {
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            assertThat(coupon.isExpired()).isTrue()
        }

        @Test
        @DisplayName("만료 일시가 미래이면 만료되지 않은 상태이다")
        fun notExpiredWhenFuture() {
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().plusDays(1))
            assertThat(coupon.isExpired()).isFalse()
        }
    }

    @Nested
    inner class CalculateDiscount {

        @Test
        @DisplayName("정액 할인: 주문 금액이 할인 금액보다 크면 할인 금액 전액을 반환한다")
        fun fixedDiscountNormal() {
            val coupon = createCoupon(type = CouponType.FIXED, value = 5000)
            val discount = coupon.calculateDiscount(Money(30000))
            assertThat(discount).isEqualTo(Money(5000))
        }

        @Test
        @DisplayName("정액 할인: 주문 금액이 할인 금액보다 작으면 주문 금액만큼만 할인한다")
        fun fixedDiscountCappedAtOrderAmount() {
            val coupon = createCoupon(type = CouponType.FIXED, value = 10000)
            val discount = coupon.calculateDiscount(Money(3000))
            assertThat(discount).isEqualTo(Money(3000))
        }

        @Test
        @DisplayName("정률 할인: 주문 금액의 비율만큼 할인한다")
        fun rateDiscountNormal() {
            val coupon = createCoupon(type = CouponType.RATE, value = 10)
            val discount = coupon.calculateDiscount(Money(30000))
            assertThat(discount).isEqualTo(Money(3000))
        }

        @Test
        @DisplayName("정률 할인: 소수점은 내림(FLOOR) 처리한다")
        fun rateDiscountFloor() {
            // 33333 * 10% = 3333.3 → 3333 (내림)
            val coupon = createCoupon(type = CouponType.RATE, value = 10)
            val discount = coupon.calculateDiscount(Money(33333))
            assertThat(discount).isEqualTo(Money(3333))
        }

        @Test
        @DisplayName("주문 금액이 0원이면 할인 금액도 0원이다")
        fun zeroOrderAmountReturnsZero() {
            val coupon = createCoupon(type = CouponType.FIXED, value = 5000)
            val discount = coupon.calculateDiscount(Money.ZERO)
            assertThat(discount).isEqualTo(Money.ZERO)
        }
    }

    @Nested
    inner class SoftDelete {

        @Test
        @DisplayName("소프트 삭제하면 deletedAt이 설정된다")
        fun success() {
            val coupon = createCoupon()
            coupon.softDelete()
            assertThat(coupon.deletedAt).isNotNull()
        }
    }
}
