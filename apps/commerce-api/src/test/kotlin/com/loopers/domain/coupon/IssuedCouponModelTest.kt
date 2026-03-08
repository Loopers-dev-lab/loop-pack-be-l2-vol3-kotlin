package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class IssuedCouponModelTest {
    companion object {
        private const val DEFAULT_COUPON_ID = 1L
        private const val DEFAULT_USER_ID = 1L
        private val DEFAULT_DISCOUNT_TYPE = DiscountType.FIXED
        private const val DEFAULT_DISCOUNT_VALUE = 5000
        private val DEFAULT_EXPIRED_AT: ZonedDateTime = ZonedDateTime.now().plusDays(7)
    }

    private fun createIssuedCouponModel(
        couponId: Long = DEFAULT_COUPON_ID,
        userId: Long = DEFAULT_USER_ID,
        discountType: DiscountType = DEFAULT_DISCOUNT_TYPE,
        discountValue: Int = DEFAULT_DISCOUNT_VALUE,
        expiredAt: ZonedDateTime = DEFAULT_EXPIRED_AT,
        status: CouponStatus = CouponStatus.AVAILABLE,
    ) = IssuedCouponModel(
        couponId = couponId,
        userId = userId,
        discountType = discountType,
        discountValue = discountValue,
        expiredAt = expiredAt,
        status = status,
    )

    @DisplayName("할인 계산")
    @Nested
    inner class CalculateDiscount {
        @DisplayName("FIXED 타입이면, 원가와 할인값 중 작은 값을 반환한다.")
        @Test
        fun returnsMinOfOriginalPriceAndDiscountValueWhenFixed() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(
                discountType = DiscountType.FIXED,
                discountValue = 5000,
            )
            val originalPrice = BigDecimal("10000")

            // act
            val result = issuedCoupon.calculateDiscount(originalPrice)

            // assert
            assertThat(result).isEqualByComparingTo(BigDecimal("5000"))
        }

        @DisplayName("FIXED 타입에서 할인값이 원가보다 크면, 원가를 반환한다.")
        @Test
        fun returnsOriginalPriceWhenFixedDiscountExceedsPrice() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(
                discountType = DiscountType.FIXED,
                discountValue = 20000,
            )
            val originalPrice = BigDecimal("10000")

            // act
            val result = issuedCoupon.calculateDiscount(originalPrice)

            // assert
            assertThat(result).isEqualByComparingTo(BigDecimal("10000"))
        }

        @DisplayName("PERCENTAGE 타입이면, 원가에 비율을 곱한 값을 반환한다.")
        @Test
        fun returnsPercentageOfOriginalPriceWhenPercentage() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10,
            )
            val originalPrice = BigDecimal("10000")

            // act
            val result = issuedCoupon.calculateDiscount(originalPrice)

            // assert
            assertThat(result).isEqualByComparingTo(BigDecimal("1000.00"))
        }
    }

    @DisplayName("유효성 검증")
    @Nested
    inner class Validate {
        @DisplayName("본인 소유, 미만료, AVAILABLE 상태이면 검증에 성공한다.")
        @Test
        fun succeedsWhenOwnedNotExpiredAndAvailable() {
            // arrange
            val issuedCoupon = createIssuedCouponModel()

            // act & assert
            assertDoesNotThrow { issuedCoupon.use(DEFAULT_USER_ID) }
        }

        @DisplayName("다른 사용자이면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedExceptionWhenUserIdMismatch() {
            // arrange
            val issuedCoupon = createIssuedCouponModel()
            val otherUserId = 999L

            // act
            val result = assertThrows<CoreException> { issuedCoupon.use(otherUserId) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenExpired() {
            // arrange
            val expiredAt = ZonedDateTime.now().minusDays(1)
            val issuedCoupon = createIssuedCouponModel(expiredAt = expiredAt)

            // act
            val result = assertThrows<CoreException> { issuedCoupon.use(DEFAULT_USER_ID) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("USED 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenAlreadyUsed() {
            // arrange
            val issuedCoupon = createIssuedCouponModel()
            issuedCoupon.use(DEFAULT_USER_ID)

            // act
            val result = assertThrows<CoreException> { issuedCoupon.use(DEFAULT_USER_ID) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("사용 복구")
    @Nested
    inner class RestoreUsage {
        @DisplayName("USED 상태이면, AVAILABLE로 변경되고 usedAt이 null이 된다.")
        @Test
        fun restoresStatusToAvailableWhenStatusIsUsed() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(status = CouponStatus.AVAILABLE)
            issuedCoupon.use(DEFAULT_USER_ID)

            // act
            issuedCoupon.restoreUsage()

            // assert
            assertAll(
                { assertThat(issuedCoupon.status).isEqualTo(CouponStatus.AVAILABLE) },
                { assertThat(issuedCoupon.usedAt).isNull() },
            )
        }

        @DisplayName("AVAILABLE 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenStatusIsAvailable() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(status = CouponStatus.AVAILABLE)

            // act & assert
            val result = assertThrows<CoreException> { issuedCoupon.restoreUsage() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("EXPIRED 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenStatusIsExpired() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(status = CouponStatus.AVAILABLE)
            issuedCoupon.expire()

            // act & assert
            val result = assertThrows<CoreException> { issuedCoupon.restoreUsage() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
