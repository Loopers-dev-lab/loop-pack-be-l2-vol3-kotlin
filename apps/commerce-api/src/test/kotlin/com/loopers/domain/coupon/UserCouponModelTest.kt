package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserCouponModelTest {

    private fun createUserCoupon(
        status: UserCouponStatus = UserCouponStatus.AVAILABLE,
    ): UserCouponModel = UserCouponModel(
        userId = 1L,
        couponTemplateId = 1L,
        status = status,
    )

    @Nested
    inner class Use {

        @Test
        fun `AVAILABLE 상태에서 use 시 USED로 변경된다`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.AVAILABLE)

            // when
            userCoupon.use()

            // then
            assertThat(userCoupon.status).isEqualTo(UserCouponStatus.USED)
        }

        @Test
        fun `USED 상태에서 use 시 에러를 반환한다`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.USED)

            // when / then
            val ex = assertThrows<CoreException> {
                userCoupon.use()
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `EXPIRED 상태에서 use 시 에러를 반환한다`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.EXPIRED)

            // when / then
            val ex = assertThrows<CoreException> {
                userCoupon.use()
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class Expire {

        @Test
        fun `AVAILABLE 상태에서 expire 시 EXPIRED로 변경된다`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.AVAILABLE)

            // when
            userCoupon.expire()

            // then
            assertThat(userCoupon.status).isEqualTo(UserCouponStatus.EXPIRED)
        }

        @Test
        fun `USED 상태에서 expire 시 에러를 반환한다`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.USED)

            // when / then
            val ex = assertThrows<CoreException> {
                userCoupon.expire()
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `EXPIRED 상태에서 expire 시 에러를 반환한다`() {
            // given
            val userCoupon = createUserCoupon(status = UserCouponStatus.EXPIRED)

            // when / then
            val ex = assertThrows<CoreException> {
                userCoupon.expire()
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
