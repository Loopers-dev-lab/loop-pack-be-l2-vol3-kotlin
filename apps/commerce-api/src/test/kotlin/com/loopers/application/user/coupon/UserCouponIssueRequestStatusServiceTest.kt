package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock

@DisplayName("UserCouponIssueRequestStatusService")
class UserCouponIssueRequestStatusServiceTest {
    private val couponIssueRequestRepository: CouponIssueRequestRepository = mock()
    private val useCase = UserCouponIssueRequestStatusService(couponIssueRequestRepository)

    private fun issuedRequest(
        id: Long = 101L,
        couponId: Long = 11L,
        userId: Long = 1L,
        issuedCouponId: Long = 555L,
    ): CouponIssueRequest = CouponIssueRequest.retrieve(
        id = id,
        couponId = couponId,
        userId = userId,
        status = CouponIssueRequest.Status.ISSUED,
        failureReasonCode = null,
        issuedCouponId = issuedCouponId,
    )

    private fun failedRequest(
        id: Long = 101L,
        couponId: Long = 11L,
        userId: Long = 1L,
        failureReasonCode: String = "COUPON_SOLD_OUT",
    ): CouponIssueRequest = CouponIssueRequest.retrieve(
        id = id,
        couponId = couponId,
        userId = userId,
        status = CouponIssueRequest.Status.FAILED,
        failureReasonCode = failureReasonCode,
        issuedCouponId = null,
    )

    @Nested
    @DisplayName("내 요청이면")
    inner class WhenOwned {
        @Test
        @DisplayName("상태 상세를 그대로 반환한다")
        fun getStatus_success() {
            given(couponIssueRequestRepository.findByIdAndUserId(101L, 1L)).willReturn(issuedRequest())

            val result = useCase.getStatus(
                UserCouponCommand.IssueRequestStatus(
                    userId = 1L,
                    requestId = 101L,
                ),
            )

            assertThat(result.requestId).isEqualTo(101L)
            assertThat(result.couponId).isEqualTo(11L)
            assertThat(result.status).isEqualTo("ISSUED")
            assertThat(result.failureReasonCode).isNull()
            assertThat(result.issuedCouponId).isEqualTo(555L)
        }

        @Test
        @DisplayName("FAILED 요청이면 failureReasonCode를 함께 반환한다")
        fun getStatus_failed() {
            given(couponIssueRequestRepository.findByIdAndUserId(101L, 1L)).willReturn(failedRequest())

            val result = useCase.getStatus(
                UserCouponCommand.IssueRequestStatus(
                    userId = 1L,
                    requestId = 101L,
                ),
            )

            assertThat(result.status).isEqualTo("FAILED")
            assertThat(result.failureReasonCode).isEqualTo("COUPON_SOLD_OUT")
            assertThat(result.issuedCouponId).isNull()
        }
    }

    @Nested
    @DisplayName("다른 사용자의 요청이거나 존재하지 않으면")
    inner class WhenNotOwned {
        @Test
        @DisplayName("COUPON_ISSUE_REQUEST_NOT_FOUND를 던진다")
        fun getStatus_notFound() {
            given(couponIssueRequestRepository.findByIdAndUserId(101L, 1L)).willReturn(null)

            val exception = assertThrows<CoreException> {
                useCase.getStatus(
                    UserCouponCommand.IssueRequestStatus(
                        userId = 1L,
                        requestId = 101L,
                    ),
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_ISSUE_REQUEST_NOT_FOUND)
        }
    }
}
