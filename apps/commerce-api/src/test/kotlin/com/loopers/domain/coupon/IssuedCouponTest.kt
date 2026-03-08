package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class IssuedCouponTest {

    @DisplayName("발급된 쿠폰의 상태를 확인할 때,")
    @Nested
    inner class Status {

        @DisplayName("사용되지 않고 만료되지 않은 쿠폰이면, AVAILABLE 상태이다.")
        @Test
        fun returnsAvailable_whenNotUsedAndNotExpired() {
            // arrange
            val issuedCoupon = IssuedCoupon(couponId = 1L, userId = 1L)
            val couponExpiresAt = ZonedDateTime.now().plusDays(30)

            // act
            val status = issuedCoupon.status(couponExpiresAt)

            // assert
            assertThat(status).isEqualTo(IssuedCouponStatus.AVAILABLE)
        }

        @DisplayName("사용된 쿠폰이면, USED 상태이다.")
        @Test
        fun returnsUsed_whenCouponIsUsed() {
            // arrange
            val issuedCoupon = IssuedCoupon(couponId = 1L, userId = 1L)
            issuedCoupon.use()
            val couponExpiresAt = ZonedDateTime.now().plusDays(30)

            // act
            val status = issuedCoupon.status(couponExpiresAt)

            // assert
            assertThat(status).isEqualTo(IssuedCouponStatus.USED)
        }

        @DisplayName("만료된 쿠폰이면, EXPIRED 상태이다.")
        @Test
        fun returnsExpired_whenCouponIsExpired() {
            // arrange
            val issuedCoupon = IssuedCoupon(couponId = 1L, userId = 1L)
            val couponExpiresAt = ZonedDateTime.now().minusDays(1)

            // act
            val status = issuedCoupon.status(couponExpiresAt)

            // assert
            assertThat(status).isEqualTo(IssuedCouponStatus.EXPIRED)
        }
    }

    @DisplayName("쿠폰 사용 가능 여부를 검증할 때,")
    @Nested
    inner class ValidateUsable {

        @DisplayName("사용 가능한 쿠폰이면, 예외가 발생하지 않는다.")
        @Test
        fun doesNotThrow_whenCouponIsAvailable() {
            // arrange
            val issuedCoupon = IssuedCoupon(couponId = 1L, userId = 1L)
            val couponExpiresAt = ZonedDateTime.now().plusDays(30)

            // act & assert
            assertDoesNotThrow {
                issuedCoupon.validateUsable(couponExpiresAt)
            }
        }

        @DisplayName("이미 사용된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsAlreadyUsed() {
            // arrange
            val issuedCoupon = IssuedCoupon(couponId = 1L, userId = 1L)
            issuedCoupon.use()
            val couponExpiresAt = ZonedDateTime.now().plusDays(30)

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.validateUsable(couponExpiresAt)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExpired() {
            // arrange
            val issuedCoupon = IssuedCoupon(couponId = 1L, userId = 1L)
            val couponExpiresAt = ZonedDateTime.now().minusDays(1)

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.validateUsable(couponExpiresAt)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
