package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class CouponIssueTest {

    private fun createCouponIssue(
        couponId: Long = 1L,
        userId: Long = 1L,
    ): CouponIssue = CouponIssue(
        couponId = couponId,
        userId = userId,
    )

    @Nested
    inner class CreateCouponIssue {

        @Test
        @DisplayName("올바른 정보로 발급 쿠폰을 생성하면 AVAILABLE 상태이다")
        fun success() {
            // arrange & act
            val issue = createCouponIssue()

            // assert
            assertAll(
                { assertThat(issue.couponId).isEqualTo(1L) },
                { assertThat(issue.userId).isEqualTo(1L) },
                { assertThat(issue.status).isEqualTo(CouponIssueStatus.AVAILABLE) },
                { assertThat(issue.usedAt).isNull() },
                { assertThat(issue.version).isEqualTo(0L) },
            )
        }

        @Test
        @DisplayName("couponId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroCouponIdThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCouponIssue(couponId = 0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("couponId가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeCouponIdThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCouponIssue(couponId = -1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("userId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroUserIdThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCouponIssue(userId = 0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("userId가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeUserIdThrowsBadRequest() {
            val result = assertThrows<CoreException> {
                createCouponIssue(userId = -1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class Use {

        @Test
        @DisplayName("AVAILABLE 상태에서 사용하면 USED 상태로 변경되고 usedAt이 설정된다")
        fun success() {
            // arrange
            val issue = createCouponIssue()

            // act
            issue.use()

            // assert
            assertAll(
                { assertThat(issue.status).isEqualTo(CouponIssueStatus.USED) },
                { assertThat(issue.usedAt).isNotNull() },
            )
        }

        @Test
        @DisplayName("이미 사용된 쿠폰을 다시 사용하면 BAD_REQUEST 예외가 발생한다")
        fun alreadyUsedThrowsBadRequest() {
            // arrange
            val issue = createCouponIssue()
            issue.use()

            // act
            val result = assertThrows<CoreException> {
                issue.use()
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("만료된 쿠폰을 사용하면 BAD_REQUEST 예외가 발생한다")
        fun expiredThrowsBadRequest() {
            // arrange
            val issue = createCouponIssue()
            issue.expire()

            // act
            val result = assertThrows<CoreException> {
                issue.use()
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class IsUsable {

        @Test
        @DisplayName("AVAILABLE 상태이면 사용 가능하다")
        fun availableIsUsable() {
            val issue = createCouponIssue()
            assertThat(issue.isUsable()).isTrue()
        }

        @Test
        @DisplayName("USED 상태이면 사용 불가능하다")
        fun usedIsNotUsable() {
            val issue = createCouponIssue()
            issue.use()
            assertThat(issue.isUsable()).isFalse()
        }

        @Test
        @DisplayName("EXPIRED 상태이면 사용 불가능하다")
        fun expiredIsNotUsable() {
            val issue = createCouponIssue()
            issue.expire()
            assertThat(issue.isUsable()).isFalse()
        }
    }

    @Nested
    inner class Expire {

        @Test
        @DisplayName("AVAILABLE 상태에서 만료 처리하면 EXPIRED 상태로 변경된다")
        fun availableToExpired() {
            // arrange
            val issue = createCouponIssue()

            // act
            issue.expire()

            // assert
            assertThat(issue.status).isEqualTo(CouponIssueStatus.EXPIRED)
        }

        @Test
        @DisplayName("USED 상태에서 만료 처리하면 상태가 변경되지 않는다")
        fun usedStaysUsed() {
            // arrange
            val issue = createCouponIssue()
            issue.use()

            // act
            issue.expire()

            // assert
            assertThat(issue.status).isEqualTo(CouponIssueStatus.USED)
        }
    }
}
