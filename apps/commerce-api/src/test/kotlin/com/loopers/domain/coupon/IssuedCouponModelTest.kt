package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class IssuedCouponModelTest {
    companion object {
        private const val DEFAULT_COUPON_ID = 1L
        private const val DEFAULT_USER_ID = 1L
    }

    private fun createIssuedCouponModel(
        couponId: Long = DEFAULT_COUPON_ID,
        userId: Long = DEFAULT_USER_ID,
        status: CouponStatus = CouponStatus.AVAILABLE,
    ) = IssuedCouponModel(
        couponId = couponId,
        userId = userId,
        status = status,
    )

    @DisplayName("사용 복구")
    @Nested
    inner class RestoreUsage {
        @DisplayName("USED 상태이면, AVAILABLE로 변경되고 usedAt이 null이 된다.")
        @Test
        fun restoresStatusToAvailableWhenStatusIsUsed() {
            // arrange
            val issuedCoupon = createIssuedCouponModel(status = CouponStatus.AVAILABLE)
            issuedCoupon.use()

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
