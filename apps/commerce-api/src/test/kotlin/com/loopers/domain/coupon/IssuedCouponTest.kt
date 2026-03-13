package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class IssuedCouponTest {
    @DisplayName("쿠폰을 사용할 때, ")
    @Nested
    inner class Use {
        @DisplayName("사용되지 않은 쿠폰을 정상적으로 사용한다.")
        @Test
        fun usesUnusedCoupon() {
            // arrange
            val coupon = IssuedCoupon(userId = 1L, couponTemplateId = 1L)

            // act
            coupon.use()

            // assert
            assertThat(coupon.used).isTrue()
        }

        @DisplayName("이미 사용된 쿠폰이면 예외가 발생한다.")
        @Test
        fun throwsException_whenAlreadyUsed() {
            // arrange
            val coupon = IssuedCoupon(userId = 1L, couponTemplateId = 1L)
            coupon.use()

            // act
            val exception = assertThrows<CoreException> {
                coupon.use()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("쿠폰 상태를 확인할 때, ")
    @Nested
    inner class GetStatus {
        @DisplayName("사용되지 않고 만료되지 않은 쿠폰은 AVAILABLE이다.")
        @Test
        fun returnsAvailable_whenNotUsedAndNotExpired() {
            // arrange
            val coupon = IssuedCoupon(userId = 1L, couponTemplateId = 1L)
            val expiredAt = ZonedDateTime.now().plusDays(30)

            // act
            val status = coupon.getStatus(expiredAt)

            // assert
            assertThat(status).isEqualTo(CouponStatus.AVAILABLE)
        }

        @DisplayName("사용된 쿠폰은 USED이다.")
        @Test
        fun returnsUsed_whenUsed() {
            // arrange
            val coupon = IssuedCoupon(userId = 1L, couponTemplateId = 1L)
            coupon.use()
            val expiredAt = ZonedDateTime.now().plusDays(30)

            // act
            val status = coupon.getStatus(expiredAt)

            // assert
            assertThat(status).isEqualTo(CouponStatus.USED)
        }

        @DisplayName("만료된 쿠폰은 EXPIRED이다.")
        @Test
        fun returnsExpired_whenExpired() {
            // arrange
            val coupon = IssuedCoupon(userId = 1L, couponTemplateId = 1L)
            val expiredAt = ZonedDateTime.now().minusDays(1)

            // act
            val status = coupon.getStatus(expiredAt)

            // assert
            assertThat(status).isEqualTo(CouponStatus.EXPIRED)
        }

        @DisplayName("사용되고 만료된 쿠폰은 USED이다.")
        @Test
        fun returnsUsed_whenUsedAndExpired() {
            // arrange
            val coupon = IssuedCoupon(userId = 1L, couponTemplateId = 1L)
            coupon.use()
            val expiredAt = ZonedDateTime.now().minusDays(1)

            // act
            val status = coupon.getStatus(expiredAt)

            // assert
            assertThat(status).isEqualTo(CouponStatus.USED)
        }
    }
}
