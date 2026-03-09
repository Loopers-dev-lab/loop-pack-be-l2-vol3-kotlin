package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponTest {

    @DisplayName("쿠폰을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("FIXED 타입으로 정상 생성된다.")
        @Test
        fun createsCoupon_whenFixedType() {
            // arrange
            val expiredAt = ZonedDateTime.now().plusDays(30)

            // act
            val coupon = Coupon(
                name = "신규 가입 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = expiredAt,
            )

            // assert
            assertAll(
                { assertThat(coupon.name).isEqualTo("신규 가입 쿠폰") },
                { assertThat(coupon.type).isEqualTo(CouponType.FIXED) },
                { assertThat(coupon.value).isEqualByComparingTo(BigDecimal("5000")) },
                { assertThat(coupon.minOrderAmount).isEqualByComparingTo(BigDecimal("10000")) },
                { assertThat(coupon.expiredAt).isEqualTo(expiredAt) },
            )
        }

        @DisplayName("RATE 타입으로 정상 생성된다.")
        @Test
        fun createsCoupon_whenRateType() {
            // arrange
            val expiredAt = ZonedDateTime.now().plusDays(30)

            // act
            val coupon = Coupon(
                name = "10% 할인 쿠폰",
                type = CouponType.RATE,
                value = BigDecimal("10"),
                minOrderAmount = null,
                expiredAt = expiredAt,
            )

            // assert
            assertAll(
                { assertThat(coupon.name).isEqualTo("10% 할인 쿠폰") },
                { assertThat(coupon.type).isEqualTo(CouponType.RATE) },
                { assertThat(coupon.value).isEqualByComparingTo(BigDecimal("10")) },
                { assertThat(coupon.minOrderAmount).isNull() },
            )
        }

        @DisplayName("value가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenValueIsZeroOrLess() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                Coupon(
                    name = "잘못된 쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal.ZERO,
                    minOrderAmount = null,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("RATE 타입에서 value가 100 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenRateValueExceeds100() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                Coupon(
                    name = "잘못된 쿠폰",
                    type = CouponType.RATE,
                    value = BigDecimal("101"),
                    minOrderAmount = null,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    inner class CalculateDiscount {

        @DisplayName("FIXED 타입이면, value를 그대로 반환한다.")
        @Test
        fun returnsValue_whenFixedType() {
            // arrange
            val coupon = Coupon(
                name = "5000원 할인",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val discount = coupon.calculateDiscount(BigDecimal("50000"))

            // assert
            assertThat(discount).isEqualByComparingTo(BigDecimal("5000"))
        }

        @DisplayName("RATE 타입이면, 주문 금액의 퍼센트를 반환한다.")
        @Test
        fun returnsPercentage_whenRateType() {
            // arrange
            val coupon = Coupon(
                name = "10% 할인",
                type = CouponType.RATE,
                value = BigDecimal("10"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val discount = coupon.calculateDiscount(BigDecimal("50000"))

            // assert
            assertThat(discount).isEqualByComparingTo(BigDecimal("5000"))
        }

        @DisplayName("FIXED 타입에서 할인 금액이 주문 금액보다 크면, 주문 금액만큼만 할인한다.")
        @Test
        fun returnsOrderAmount_whenFixedDiscountExceedsOrderAmount() {
            // arrange
            val coupon = Coupon(
                name = "10000원 할인",
                type = CouponType.FIXED,
                value = BigDecimal("10000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val discount = coupon.calculateDiscount(BigDecimal("5000"))

            // assert
            assertThat(discount).isEqualByComparingTo(BigDecimal("5000"))
        }

        @DisplayName("RATE 100%이면, 주문 금액 전체를 할인한다.")
        @Test
        fun returnsFullAmount_whenRate100Percent() {
            // arrange
            val coupon = Coupon(
                name = "100% 할인",
                type = CouponType.RATE,
                value = BigDecimal("100"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val discount = coupon.calculateDiscount(BigDecimal("50000"))

            // assert
            assertThat(discount).isEqualByComparingTo(BigDecimal("50000"))
        }
    }

    @DisplayName("쿠폰 만료를 확인할 때,")
    @Nested
    inner class CheckExpiration {

        @DisplayName("만료되지 않은 쿠폰이면, false를 반환한다.")
        @Test
        fun returnsFalse_whenNotExpired() {
            // arrange
            val coupon = Coupon(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val result = coupon.isExpired()

            // assert
            assertThat(result).isFalse()
        }

        @DisplayName("만료된 쿠폰이면, true를 반환한다.")
        @Test
        fun returnsTrue_whenExpired() {
            // arrange
            val coupon = Coupon(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().minusDays(1),
            )

            // act
            val result = coupon.isExpired()

            // assert
            assertThat(result).isTrue()
        }
    }

    @DisplayName("최소 주문 금액을 검증할 때,")
    @Nested
    inner class ValidateMinOrderAmount {

        @DisplayName("minOrderAmount 이상이면, 통과한다.")
        @Test
        fun passes_whenOrderAmountMeetsMinimum() {
            // arrange
            val coupon = Coupon(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act & assert (예외 없이 통과)
            coupon.validateMinOrderAmount(BigDecimal("10000"))
        }

        @DisplayName("minOrderAmount 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenOrderAmountBelowMinimum() {
            // arrange
            val coupon = Coupon(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val exception = assertThrows<CoreException> {
                coupon.validateMinOrderAmount(BigDecimal("9999"))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("minOrderAmount가 null이면, 항상 통과한다.")
        @Test
        fun passes_whenMinOrderAmountIsNull() {
            // arrange
            val coupon = Coupon(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act & assert (예외 없이 통과)
            coupon.validateMinOrderAmount(BigDecimal("1"))
        }
    }
}
