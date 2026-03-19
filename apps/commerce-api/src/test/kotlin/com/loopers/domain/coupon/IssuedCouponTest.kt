package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class IssuedCouponTest {

    @Nested
    inner class Reserve {
        @Test
        fun `AVAILABLE_상태에서_예약할_수_있다`() {
            val issuedCoupon = createIssuedCoupon()

            issuedCoupon.reserve()

            assertThat(issuedCoupon.status).isEqualTo(CouponStatus.RESERVED)
        }

        @Test
        fun `USED_상태에서_예약하면_예외가_발생한다`() {
            val issuedCoupon = createIssuedCoupon(status = CouponStatus.USED)

            val result = assertThrows<CoreException> { issuedCoupon.reserve() }

            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_AVAILABLE)
        }

        @Test
        fun `EXPIRED_상태에서_예약하면_예외가_발생한다`() {
            val issuedCoupon = createIssuedCoupon(status = CouponStatus.EXPIRED)

            val result = assertThrows<CoreException> { issuedCoupon.reserve() }

            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_AVAILABLE)
        }
    }

    @Nested
    inner class ConfirmUse {
        @Test
        fun `RESERVED_상태에서_사용_확정할_수_있다`() {
            val issuedCoupon = createIssuedCoupon(status = CouponStatus.RESERVED)

            issuedCoupon.confirmUse()

            assertThat(issuedCoupon.status).isEqualTo(CouponStatus.USED)
        }

        @Test
        fun `AVAILABLE_상태에서_사용_확정하면_예외가_발생한다`() {
            val issuedCoupon = createIssuedCoupon()

            val result = assertThrows<CoreException> { issuedCoupon.confirmUse() }

            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_AVAILABLE)
        }
    }

    @Nested
    inner class Release {
        @Test
        fun `RESERVED_상태에서_예약을_해제할_수_있다`() {
            val issuedCoupon = createIssuedCoupon(status = CouponStatus.RESERVED)

            issuedCoupon.release()

            assertThat(issuedCoupon.status).isEqualTo(CouponStatus.AVAILABLE)
        }

        @Test
        fun `AVAILABLE_상태에서_예약을_해제하면_예외가_발생한다`() {
            val issuedCoupon = createIssuedCoupon()

            val result = assertThrows<CoreException> { issuedCoupon.release() }

            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_AVAILABLE)
        }
    }

    @Nested
    inner class ValidateOwner {
        @Test
        fun `소유자가_일치하면_예외가_발생하지_않는다`() {
            val issuedCoupon = createIssuedCoupon(memberId = 1L)
            issuedCoupon.validateOwner(1L)
        }

        @Test
        fun `소유자가_일치하지_않으면_예외가_발생한다`() {
            val issuedCoupon = createIssuedCoupon(memberId = 1L)
            val result = assertThrows<CoreException> { issuedCoupon.validateOwner(2L) }
            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_OWNER)
        }
    }

    @Nested
    inner class ValidateUsable {
        @Test
        fun `AVAILABLE_상태면_예외가_발생하지_않는다`() {
            val issuedCoupon = createIssuedCoupon()
            issuedCoupon.validateUsable()
        }

        @Test
        fun `RESERVED_상태면_예외가_발생한다`() {
            val issuedCoupon = createIssuedCoupon(status = CouponStatus.RESERVED)
            val result = assertThrows<CoreException> { issuedCoupon.validateUsable() }
            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_AVAILABLE)
        }
    }

    private fun createIssuedCoupon(
        memberId: Long = 1L,
        status: CouponStatus = CouponStatus.AVAILABLE,
    ): IssuedCoupon {
        return IssuedCoupon(
            couponId = 1L,
            memberId = memberId,
            status = status,
            issuedAt = ZonedDateTime.now(),
        )
    }
}
