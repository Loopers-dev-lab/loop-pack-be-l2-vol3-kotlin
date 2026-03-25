package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class CouponIssueRequestTest {

    @DisplayName("발급 요청을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 값이 주어지면, PENDING 상태로 생성된다.")
        @Test
        fun createsCouponIssueRequest_withPendingStatus() {
            // arrange
            val requestId = "550e8400-e29b-41d4-a716-446655440000"
            val couponId = 1L
            val userId = 100L

            // act
            val request = CouponIssueRequest(
                requestId = requestId,
                couponId = couponId,
                userId = userId,
            )

            // assert
            assertAll(
                { assertThat(request.requestId).isEqualTo(requestId) },
                { assertThat(request.couponId).isEqualTo(couponId) },
                { assertThat(request.userId).isEqualTo(userId) },
                { assertThat(request.status).isEqualTo(CouponIssueStatus.PENDING) },
                { assertThat(request.failReason).isNull() },
            )
        }
    }

    @DisplayName("발급 요청 상태를 전이할 때,")
    @Nested
    inner class MarkIssued {

        @DisplayName("PENDING 상태에서 markIssued()를 호출하면, ISSUED 상태로 전이된다.")
        @Test
        fun transitionsToIssued_whenStatusIsPending() {
            // arrange
            val request = CouponIssueRequest(
                requestId = "550e8400-e29b-41d4-a716-446655440000",
                couponId = 1L,
                userId = 100L,
            )

            // act
            request.markIssued()

            // assert
            assertThat(request.status).isEqualTo(CouponIssueStatus.ISSUED)
        }
    }

    @DisplayName("발급 요청을 실패 처리할 때,")
    @Nested
    inner class MarkFailed {

        @DisplayName("PENDING 상태에서 markFailed()를 호출하면, FAILED 상태로 전이되고 failReason이 설정된다.")
        @Test
        fun transitionsToFailed_whenStatusIsPending() {
            // arrange
            val request = CouponIssueRequest(
                requestId = "550e8400-e29b-41d4-a716-446655440000",
                couponId = 1L,
                userId = 100L,
            )
            val reason = "수량 소진"

            // act
            request.markFailed(reason)

            // assert
            assertAll(
                { assertThat(request.status).isEqualTo(CouponIssueStatus.FAILED) },
                { assertThat(request.failReason).isEqualTo(reason) },
            )
        }
    }

    @DisplayName("PENDING이 아닌 상태에서 상태 전이를 시도할 때,")
    @Nested
    inner class InvalidStateTransition {

        @DisplayName("ISSUED 상태에서 markIssued()를 호출하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenMarkIssuedOnIssuedStatus() {
            // arrange
            val request = CouponIssueRequest(
                requestId = "550e8400-e29b-41d4-a716-446655440000",
                couponId = 1L,
                userId = 100L,
            )
            request.markIssued()

            // act & assert
            val exception = assertThrows<CoreException> {
                request.markIssued()
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("ISSUED 상태에서 markFailed()를 호출하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenMarkFailedOnIssuedStatus() {
            // arrange
            val request = CouponIssueRequest(
                requestId = "550e8400-e29b-41d4-a716-446655440000",
                couponId = 1L,
                userId = 100L,
            )
            request.markIssued()

            // act & assert
            val exception = assertThrows<CoreException> {
                request.markFailed("수량 소진")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("FAILED 상태에서 markIssued()를 호출하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenMarkIssuedOnFailedStatus() {
            // arrange
            val request = CouponIssueRequest(
                requestId = "550e8400-e29b-41d4-a716-446655440000",
                couponId = 1L,
                userId = 100L,
            )
            request.markFailed("수량 소진")

            // act & assert
            val exception = assertThrows<CoreException> {
                request.markIssued()
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }
}
