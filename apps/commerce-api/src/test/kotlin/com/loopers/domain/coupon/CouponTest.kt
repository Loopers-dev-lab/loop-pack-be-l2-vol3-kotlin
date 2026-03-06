package com.loopers.domain.coupon

import com.loopers.domain.common.vo.Money
import com.loopers.domain.coupon.model.Coupon
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponTest {

    @Nested
    @DisplayName("issue 시")
    inner class Issue {

        private fun createCoupon(
            totalQuantity: Int? = 100,
            expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        ): Coupon {
            return Coupon(
                name = "테스트 쿠폰",
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                totalQuantity = totalQuantity,
                expiredAt = expiredAt,
            )
        }

        @Test
        @DisplayName("정상 쿠폰에 issue() 호출 시 issuedCount가 증가한다")
        fun issue_normal_incrementsIssuedCount() {
            // arrange
            val coupon = createCoupon()

            // act
            coupon.issue()

            // assert
            assertThat(coupon.issuedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("삭제된 쿠폰에 issue() 호출 시 BAD_REQUEST 예외가 발생한다")
        fun issue_deleted_throwsException() {
            // arrange
            val coupon = createCoupon()
            coupon.delete()

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).contains("삭제된")
        }

        @Test
        @DisplayName("만료된 쿠폰에 issue() 호출 시 BAD_REQUEST 예외가 발생한다")
        fun issue_expired_throwsException() {
            // arrange
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).contains("만료된")
        }

        @Test
        @DisplayName("발급 수량이 초과되면 BAD_REQUEST 예외가 발생한다")
        fun issue_quantityExceeded_throwsException() {
            // arrange
            val coupon = createCoupon(totalQuantity = 1)
            coupon.issue() // 1번째 발급

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue() // 2번째 발급 시도
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).contains("수량")
        }
    }

    @Nested
    @DisplayName("isExpired 시")
    inner class IsExpired {

        @Test
        @DisplayName("만료 시각이 현재보다 이전이면 만료로 판단한다")
        fun isExpired_exactlyAtExpiredAt_returnsTrue() {
            // arrange
            val expiredAt = ZonedDateTime.now().minusNanos(1) // 현재보다 1ns 이전 = 이미 지남
            val coupon = Coupon(
                name = "테스트 쿠폰",
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                expiredAt = expiredAt,
            )

            // act
            val result = coupon.isExpired()

            // assert
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("calculateDiscount 시")
    inner class CalculateDiscount {

        private fun fixedCoupon(
            discountAmount: Long,
            minOrderAmount: Money? = null,
        ): Coupon = Coupon(
            name = "고정 할인 쿠폰",
            type = Coupon.CouponType.FIXED,
            value = discountAmount,
            minOrderAmount = minOrderAmount,
            expiredAt = ZonedDateTime.now().plusDays(30),
        )

        private fun rateCoupon(
            ratePercent: Long,
            minOrderAmount: Money? = null,
            maxDiscount: Money? = null,
        ): Coupon = Coupon(
            name = "비율 할인 쿠폰",
            type = Coupon.CouponType.RATE,
            value = ratePercent,
            minOrderAmount = minOrderAmount,
            maxDiscount = maxDiscount,
            expiredAt = ZonedDateTime.now().plusDays(30),
        )

        @Test
        @DisplayName("FIXED 쿠폰 — minOrderAmount 미달 시 BAD_REQUEST 예외가 발생한다")
        fun calculateDiscount_fixed_belowMinOrderAmount_throwsBadRequest() {
            // arrange
            val coupon = fixedCoupon(
                discountAmount = 1000L,
                minOrderAmount = Money(BigDecimal("5000")),
            )
            val orderAmount = Money(BigDecimal("4999"))

            // act & assert
            val exception = assertThrows<CoreException> {
                coupon.calculateDiscount(orderAmount)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("RATE 쿠폰 — minOrderAmount 미달 시 BAD_REQUEST 예외가 발생한다")
        fun calculateDiscount_rate_belowMinOrderAmount_throwsBadRequest() {
            // arrange
            val coupon = rateCoupon(
                ratePercent = 10L,
                minOrderAmount = Money(BigDecimal("5000")),
            )
            val orderAmount = Money(BigDecimal("4999"))

            // act & assert
            val exception = assertThrows<CoreException> {
                coupon.calculateDiscount(orderAmount)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("FIXED 쿠폰 — minOrderAmount와 동일한 금액이면 정상 할인을 반환한다")
        fun calculateDiscount_fixed_exactlyMinOrderAmount_returnsDiscount() {
            // arrange
            val coupon = fixedCoupon(
                discountAmount = 1000L,
                minOrderAmount = Money(BigDecimal("5000")),
            )
            val orderAmount = Money(BigDecimal("5000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("1000"))
        }

        @Test
        @DisplayName("RATE 쿠폰 — minOrderAmount와 동일한 금액이면 정상 할인을 반환한다")
        fun calculateDiscount_rate_exactlyMinOrderAmount_returnsDiscount() {
            // arrange
            val coupon = rateCoupon(
                ratePercent = 10L,
                minOrderAmount = Money(BigDecimal("5000")),
            )
            val orderAmount = Money(BigDecimal("5000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("500"))
        }

        @Test
        @DisplayName("FIXED 쿠폰 — minOrderAmount 초과 시 정상 할인을 반환한다")
        fun calculateDiscount_fixed_aboveMinOrderAmount_returnsDiscount() {
            // arrange
            val coupon = fixedCoupon(
                discountAmount = 1000L,
                minOrderAmount = Money(BigDecimal("5000")),
            )
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("1000"))
        }

        @Test
        @DisplayName("RATE 쿠폰 — minOrderAmount 초과 시 정상 할인을 반환한다")
        fun calculateDiscount_rate_aboveMinOrderAmount_returnsDiscount() {
            // arrange
            val coupon = rateCoupon(
                ratePercent = 10L,
                minOrderAmount = Money(BigDecimal("5000")),
            )
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("1000"))
        }
    }
}
