package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CouponIssueModel")
class CouponIssueModelTest {

    companion object {
        private const val COUPON_ID = 1L
        private const val USER_ID = 1L
    }

    private fun createCouponIssue(
        couponId: Long = COUPON_ID,
        userId: Long = USER_ID,
        status: CouponIssueStatus = CouponIssueStatus.AVAILABLE,
    ): CouponIssueModel = CouponIssueModel(
        couponId = couponId,
        userId = userId,
        status = status,
    )

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("기본 상태는 AVAILABLE이다")
        @Test
        fun createsWithAvailableStatus() {
            // arrange & act
            val issue = createCouponIssue()

            // assert
            assertThat(issue.couponId).isEqualTo(COUPON_ID)
            assertThat(issue.userId).isEqualTo(USER_ID)
            assertThat(issue.status).isEqualTo(CouponIssueStatus.AVAILABLE)
        }
    }

    @DisplayName("쿠폰 사용")
    @Nested
    inner class Use {
        @DisplayName("AVAILABLE 상태에서 use() 호출하면 USED로 변경된다")
        @Test
        fun changesStatusToUsed_whenAvailable() {
            // arrange
            val issue = createCouponIssue()

            // act
            issue.use()

            // assert
            assertThat(issue.status).isEqualTo(CouponIssueStatus.USED)
        }

        @DisplayName("USED 상태에서 use() 호출하면 예외가 발생한다")
        @Test
        fun throwsException_whenAlreadyUsed() {
            // arrange
            val issue = createCouponIssue(status = CouponIssueStatus.USED)

            // act & assert
            assertThatThrownBy { issue.use() }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("사용할 수 없는 쿠폰")
        }

        @DisplayName("EXPIRED 상태에서 use() 호출하면 예외가 발생한다")
        @Test
        fun throwsException_whenExpired() {
            // arrange
            val issue = createCouponIssue(status = CouponIssueStatus.EXPIRED)

            // act & assert
            assertThatThrownBy { issue.use() }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("사용할 수 없는 쿠폰")
        }
    }

    @DisplayName("사용 가능 여부")
    @Nested
    inner class IsUsable {
        @DisplayName("AVAILABLE 상태이면 true를 반환한다")
        @Test
        fun returnsTrue_whenAvailable() {
            val issue = createCouponIssue(status = CouponIssueStatus.AVAILABLE)
            assertThat(issue.isUsable()).isTrue()
        }

        @DisplayName("USED 상태이면 false를 반환한다")
        @Test
        fun returnsFalse_whenUsed() {
            val issue = createCouponIssue(status = CouponIssueStatus.USED)
            assertThat(issue.isUsable()).isFalse()
        }

        @DisplayName("EXPIRED 상태이면 false를 반환한다")
        @Test
        fun returnsFalse_whenExpired() {
            val issue = createCouponIssue(status = CouponIssueStatus.EXPIRED)
            assertThat(issue.isUsable()).isFalse()
        }
    }
}
