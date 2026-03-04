package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class IssuedCouponTest {

    private fun createIssuedCoupon(
        couponId: Long = 1L,
        userId: Long = 1L,
        status: IssuedCouponStatus = IssuedCouponStatus.AVAILABLE,
    ): IssuedCoupon {
        return IssuedCoupon(
            couponId = couponId,
            userId = userId,
            status = status,
        )
    }

    @DisplayName("쿠폰을 사용할 때,")
    @Nested
    inner class Use {

        @DisplayName("AVAILABLE 상태에서 사용하면, USED 상태가 되고 usedAt이 설정된다.")
        @Test
        fun changesStatusToUsed_whenAvailable() {
            // arrange
            val issuedCoupon = createIssuedCoupon()

            // act
            issuedCoupon.use()

            // assert
            assertAll(
                { assertThat(issuedCoupon.status).isEqualTo(IssuedCouponStatus.USED) },
                { assertThat(issuedCoupon.usedAt).isNotNull() },
            )
        }

        @DisplayName("USED 상태에서 사용 시도하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenAlreadyUsed() {
            // arrange
            val issuedCoupon = createIssuedCoupon(status = IssuedCouponStatus.USED)

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.use()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("쿠폰 사용 가능 여부를 확인할 때,")
    @Nested
    inner class IsUsable {

        @DisplayName("AVAILABLE 상태이면, true를 반환한다.")
        @Test
        fun returnsTrue_whenAvailable() {
            // arrange
            val issuedCoupon = createIssuedCoupon()

            // act
            val result = issuedCoupon.isUsable()

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("USED 상태이면, false를 반환한다.")
        @Test
        fun returnsFalse_whenUsed() {
            // arrange
            val issuedCoupon = createIssuedCoupon(status = IssuedCouponStatus.USED)

            // act
            val result = issuedCoupon.isUsable()

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("쿠폰 소유자를 검증할 때,")
    @Nested
    inner class ValidateOwner {

        @DisplayName("본인 쿠폰이면, 통과한다.")
        @Test
        fun passes_whenOwnerMatches() {
            // arrange
            val issuedCoupon = createIssuedCoupon(userId = 1L)

            // act & assert (예외 없이 통과)
            issuedCoupon.validateOwner(1L)
        }

        @DisplayName("타인 쿠폰이면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsException_whenOwnerDoesNotMatch() {
            // arrange
            val issuedCoupon = createIssuedCoupon(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.validateOwner(2L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @DisplayName("쿠폰 사용 가능 상태를 검증할 때,")
    @Nested
    inner class ValidateUsable {

        @DisplayName("AVAILABLE 상태이면, 통과한다.")
        @Test
        fun passes_whenAvailable() {
            // arrange
            val issuedCoupon = createIssuedCoupon()

            // act & assert (예외 없이 통과)
            issuedCoupon.validateUsable()
        }

        @DisplayName("USED 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenUsed() {
            // arrange
            val issuedCoupon = createIssuedCoupon(status = IssuedCouponStatus.USED)

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.validateUsable()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
