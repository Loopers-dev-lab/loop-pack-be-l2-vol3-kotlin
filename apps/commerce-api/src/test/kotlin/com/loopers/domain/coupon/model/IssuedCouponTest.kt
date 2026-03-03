package com.loopers.domain.coupon.model

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class IssuedCouponTest {

    private fun issuedCoupon(
        refCouponId: CouponId = CouponId(1L),
        refUserId: UserId = UserId(1L),
        status: IssuedCoupon.CouponStatus = IssuedCoupon.CouponStatus.AVAILABLE,
        usedAt: ZonedDateTime? = null,
        createdAt: ZonedDateTime = ZonedDateTime.now(),
    ) = IssuedCoupon(
        refCouponId = refCouponId,
        refUserId = refUserId,
        status = status,
        usedAt = usedAt,
        createdAt = createdAt,
    )

    @Nested
    @DisplayName("use 호출 시")
    inner class Use {

        @Test
        @DisplayName("AVAILABLE 상태에서 use() 호출 시 USED로 전환되고 usedAt이 설정된다")
        fun use_fromAvailable_statusChangesToUsedAndUsedAtSet() {
            // arrange
            val issuedCoupon = issuedCoupon(status = IssuedCoupon.CouponStatus.AVAILABLE)

            // act
            issuedCoupon.use()

            // assert
            assertThat(issuedCoupon.status).isEqualTo(IssuedCoupon.CouponStatus.USED)
            assertThat(issuedCoupon.usedAt).isNotNull()
        }

        @Test
        @DisplayName("이미 USED인 쿠폰에 use() 호출 시 BAD_REQUEST 예외가 발생한다")
        fun use_fromUsed_throwsException() {
            // arrange
            val issuedCoupon = issuedCoupon(status = IssuedCoupon.CouponStatus.USED, usedAt = ZonedDateTime.now())

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.use()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("사용할 수 없는 쿠폰입니다.")
        }

        @Test
        @DisplayName("EXPIRED 상태의 쿠폰에 use() 호출 시 BAD_REQUEST 예외가 발생한다")
        fun use_fromExpired_throwsException() {
            // arrange
            val issuedCoupon = issuedCoupon(status = IssuedCoupon.CouponStatus.EXPIRED)

            // act
            val exception = assertThrows<CoreException> {
                issuedCoupon.use()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("사용할 수 없는 쿠폰입니다.")
        }
    }

    @Nested
    @DisplayName("isAvailable 확인 시")
    inner class IsAvailable {

        @Test
        @DisplayName("AVAILABLE 상태이면 true를 반환한다")
        fun isAvailable_available_returnsTrue() {
            // arrange
            val issuedCoupon = issuedCoupon(status = IssuedCoupon.CouponStatus.AVAILABLE)

            // assert
            assertThat(issuedCoupon.isAvailable()).isTrue()
        }

        @Test
        @DisplayName("USED 상태이면 false를 반환한다")
        fun isAvailable_used_returnsFalse() {
            // arrange
            val issuedCoupon = issuedCoupon(status = IssuedCoupon.CouponStatus.USED)

            // assert
            assertThat(issuedCoupon.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("EXPIRED 상태이면 false를 반환한다")
        fun isAvailable_expired_returnsFalse() {
            // arrange
            val issuedCoupon = issuedCoupon(status = IssuedCoupon.CouponStatus.EXPIRED)

            // assert
            assertThat(issuedCoupon.isAvailable()).isFalse()
        }
    }

    @Nested
    @DisplayName("isOwnedBy 확인 시")
    inner class IsOwnedBy {

        @Test
        @DisplayName("본인 userId이면 true를 반환한다")
        fun isOwnedBy_ownUser_returnsTrue() {
            // arrange
            val userId = UserId(42L)
            val issuedCoupon = issuedCoupon(refUserId = userId)

            // assert
            assertThat(issuedCoupon.isOwnedBy(userId)).isTrue()
        }

        @Test
        @DisplayName("타인의 userId이면 false를 반환한다")
        fun isOwnedBy_otherUser_returnsFalse() {
            // arrange
            val ownerUserId = UserId(42L)
            val otherUserId = UserId(99L)
            val issuedCoupon = issuedCoupon(refUserId = ownerUserId)

            // assert
            assertThat(issuedCoupon.isOwnedBy(otherUserId)).isFalse()
        }
    }
}
