package com.loopers.domain.coupon

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class IssuedCouponModelRestoreTest {
    private fun createIssuedCoupon(
        status: CouponStatus = CouponStatus.USED,
        usedAt: ZonedDateTime? = ZonedDateTime.now(),
    ) = IssuedCouponModel(
        id = 1L,
        couponTemplateId = 1L,
        memberId = 1L,
        status = status,
        expiredAt = ZonedDateTime.now().plusDays(7),
        usedAt = usedAt,
    )

    @DisplayName("사용된 쿠폰을 복원할 때,")
    @Nested
    inner class Restore {
        @DisplayName("USED 상태이면, AVAILABLE로 복원되고 usedAt이 null이 된다.")
        @Test
        fun restoresToAvailable_whenUsed() {
            // arrange
            val coupon = createIssuedCoupon(status = CouponStatus.USED)

            // act
            val restored = coupon.restore()

            // assert
            assertAll(
                { assertThat(restored.status).isEqualTo(CouponStatus.AVAILABLE) },
                { assertThat(restored.usedAt).isNull() },
            )
        }

        @DisplayName("AVAILABLE 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAlreadyAvailable() {
            // arrange
            val coupon = createIssuedCoupon(status = CouponStatus.AVAILABLE, usedAt = null)

            // act & assert
            val result = assertThrows<CoreException> { coupon.restore() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
