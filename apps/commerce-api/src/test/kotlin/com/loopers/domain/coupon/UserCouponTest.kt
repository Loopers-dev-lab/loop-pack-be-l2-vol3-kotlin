package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class UserCouponTest {

    private val validExpiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30)

    @DisplayName("발급 쿠폰 생성")
    @Nested
    inner class Create {

        @DisplayName("정상 생성 시 AVAILABLE 상태이다")
        @Test
        fun success() {
            val userCoupon = UserCoupon.create(couponId = 1L, userId = 1L, expiredAt = validExpiredAt)

            assertAll(
                { assertThat(userCoupon.couponId).isEqualTo(1L) },
                { assertThat(userCoupon.userId).isEqualTo(1L) },
                { assertThat(userCoupon.status).isEqualTo(UserCouponStatus.AVAILABLE) },
                { assertThat(userCoupon.usedAt).isNull() },
                { assertThat(userCoupon.usedOrderId).isNull() },
            )
        }
    }

    @DisplayName("사용 가능 검증")
    @Nested
    inner class ValidateUsableBy {

        @DisplayName("본인 소유 + AVAILABLE + 미만료이면 통과한다")
        @Test
        fun success() {
            val userCoupon = UserCoupon.create(couponId = 1L, userId = 1L, expiredAt = validExpiredAt)

            userCoupon.validateUsableBy(1L)
        }

        @DisplayName("다른 유저가 사용하면 COUPON_NOT_OWNED 에러가 발생한다")
        @Test
        fun failWhenNotOwned() {
            val userCoupon = UserCoupon.create(couponId = 1L, userId = 1L, expiredAt = validExpiredAt)

            val exception = assertThrows<CoreException> {
                userCoupon.validateUsableBy(2L)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_OWNED)
        }

        @DisplayName("이미 사용된 쿠폰이면 COUPON_ALREADY_USED 에러가 발생한다")
        @Test
        fun failWhenAlreadyUsed() {
            val userCoupon = UserCoupon.create(couponId = 1L, userId = 1L, expiredAt = validExpiredAt)
            userCoupon.use(orderId = 100L)

            val exception = assertThrows<CoreException> {
                userCoupon.validateUsableBy(1L)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_ALREADY_USED)
        }

        @DisplayName("만료된 쿠폰이면 COUPON_EXPIRED 에러가 발생한다")
        @Test
        fun failWhenExpired() {
            val userCoupon = UserCoupon.create(
                couponId = 1L,
                userId = 1L,
                expiredAt = ZonedDateTime.now().minusDays(1),
            )

            val exception = assertThrows<CoreException> {
                userCoupon.validateUsableBy(1L)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_EXPIRED)
        }
    }

    @DisplayName("쿠폰 사용")
    @Nested
    inner class Use {

        @DisplayName("사용하면 USED 상태가 되고 사용 정보가 기록된다")
        @Test
        fun success() {
            val userCoupon = UserCoupon.create(couponId = 1L, userId = 1L, expiredAt = validExpiredAt)

            userCoupon.use(orderId = 100L)

            assertAll(
                { assertThat(userCoupon.status).isEqualTo(UserCouponStatus.USED) },
                { assertThat(userCoupon.usedAt).isNotNull() },
                { assertThat(userCoupon.usedOrderId).isEqualTo(100L) },
            )
        }
    }
}
