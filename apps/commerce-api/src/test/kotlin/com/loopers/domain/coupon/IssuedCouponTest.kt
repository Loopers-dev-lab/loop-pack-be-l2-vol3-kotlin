package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

@DisplayName("IssuedCoupon 도메인")
class IssuedCouponTest {
    private val futureDate = ZonedDateTime.now().plusDays(30)
    private val pastDate = ZonedDateTime.now().minusDays(1)

    @Nested
    @DisplayName("유효한 쿠폰 템플릿에서 발급하면 AVAILABLE 상태로 생성된다")
    inner class WhenIssued {
        @Test
        @DisplayName("status=AVAILABLE, expiredAt=Coupon.expiredAt 복사 확인")
        fun issue_success() {
            val issuedCoupon = IssuedCoupon.issue(
                couponId = 1L,
                userId = 100L,
                expiredAt = futureDate,
            )

            assertAll(
                { assertThat(issuedCoupon.id).isNull() },
                { assertThat(issuedCoupon.couponId).isEqualTo(1L) },
                { assertThat(issuedCoupon.userId).isEqualTo(100L) },
                { assertThat(issuedCoupon.status).isEqualTo(IssuedCoupon.Status.AVAILABLE) },
                { assertThat(issuedCoupon.expiredAt).isEqualTo(futureDate) },
                { assertThat(issuedCoupon.usedAt).isNull() },
            )
        }
    }

    @Nested
    @DisplayName("USED 상태 쿠폰은 use() 호출 시 실패한다")
    inner class WhenAlreadyUsed {
        @Test
        @DisplayName("이미 USED → 예외")
        fun use_alreadyUsed() {
            val issuedCoupon = IssuedCoupon.retrieve(
                id = 1L,
                couponId = 1L,
                userId = 100L,
                status = IssuedCoupon.Status.USED,
                expiredAt = futureDate,
                usedAt = ZonedDateTime.now().minusHours(1),
            )

            val exception = assertThrows<CoreException> { issuedCoupon.use() }

            assertThat(exception.errorType).isEqualTo(ErrorType.ISSUED_COUPON_ALREADY_USED)
        }
    }

    @Nested
    @DisplayName("만료된 쿠폰은 use() 호출 시 실패한다")
    inner class WhenExpired {
        @Test
        @DisplayName("expiredAt 경과 → 예외")
        fun use_expired() {
            val issuedCoupon = IssuedCoupon.retrieve(
                id = 1L,
                couponId = 1L,
                userId = 100L,
                status = IssuedCoupon.Status.AVAILABLE,
                expiredAt = pastDate,
                usedAt = null,
            )

            val exception = assertThrows<CoreException> { issuedCoupon.use() }

            assertThat(exception.errorType).isEqualTo(ErrorType.ISSUED_COUPON_EXPIRED)
        }
    }

    @Nested
    @DisplayName("use() 성공 시 USED 상태로 전이된다")
    inner class WhenUseSuccess {
        @Test
        @DisplayName("AVAILABLE + 미만료 → USED, usedAt 기록")
        fun use_success() {
            val issuedCoupon = IssuedCoupon.issue(
                couponId = 1L,
                userId = 100L,
                expiredAt = futureDate,
            )

            val used = issuedCoupon.use()

            assertAll(
                { assertThat(used.status).isEqualTo(IssuedCoupon.Status.USED) },
                { assertThat(used.usedAt).isNotNull() },
            )
        }
    }

    @Nested
    @DisplayName("displayStatus() 검증")
    inner class DisplayStatus {
        @Test
        @DisplayName("AVAILABLE + 미만료 → AVAILABLE")
        fun displayStatus_available() {
            val issuedCoupon = IssuedCoupon.retrieve(
                id = 1L,
                couponId = 1L,
                userId = 100L,
                status = IssuedCoupon.Status.AVAILABLE,
                expiredAt = futureDate,
                usedAt = null,
            )

            assertThat(issuedCoupon.displayStatus()).isEqualTo(IssuedCoupon.DisplayStatus.AVAILABLE)
        }

        @Test
        @DisplayName("USED → USED")
        fun displayStatus_used() {
            val issuedCoupon = IssuedCoupon.retrieve(
                id = 1L,
                couponId = 1L,
                userId = 100L,
                status = IssuedCoupon.Status.USED,
                expiredAt = futureDate,
                usedAt = ZonedDateTime.now().minusHours(1),
            )

            assertThat(issuedCoupon.displayStatus()).isEqualTo(IssuedCoupon.DisplayStatus.USED)
        }

        @Test
        @DisplayName("AVAILABLE + 만료 → EXPIRED")
        fun displayStatus_expired() {
            val issuedCoupon = IssuedCoupon.retrieve(
                id = 1L,
                couponId = 1L,
                userId = 100L,
                status = IssuedCoupon.Status.AVAILABLE,
                expiredAt = pastDate,
                usedAt = null,
            )

            assertThat(issuedCoupon.displayStatus()).isEqualTo(IssuedCoupon.DisplayStatus.EXPIRED)
        }
    }
}
