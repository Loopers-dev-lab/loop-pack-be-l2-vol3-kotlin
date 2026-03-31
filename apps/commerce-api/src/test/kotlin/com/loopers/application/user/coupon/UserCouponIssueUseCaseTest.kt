package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.support.event.user.CouponIssueRequestedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.springframework.context.ApplicationEventPublisher

@DisplayName("UserCouponIssueUseCase")
class UserCouponIssueUseCaseTest {
    private val eventPublisher: ApplicationEventPublisher = mock()
    private val couponIssueRequestRepository: CouponIssueRequestRepository = mock()
    private val useCase = UserCouponIssueUseCase(
        eventPublisher = eventPublisher,
        couponIssueRequestRepository = couponIssueRequestRepository,
    )

    private fun requestedRequest(
        id: Long = 101L,
        couponId: Long = 11L,
        userId: Long = 1L,
    ): CouponIssueRequest = CouponIssueRequest.retrieve(
        id = id,
        couponId = couponId,
        userId = userId,
        status = CouponIssueRequest.Status.REQUESTED,
        failureReasonCode = null,
        issuedCouponId = null,
    )

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
    @DisplayName("새 쿠폰 발급 요청이면")
    inner class WhenIssueRequestMissing {
        @Test
        @DisplayName("REQUESTED 요청을 저장하고 outbox 이벤트를 발행한다")
        fun issue_success() {
            given(couponIssueRequestRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(null)
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.id).isNull()
                        assertThat(request.couponId).isEqualTo(11L)
                        assertThat(request.userId).isEqualTo(1L)
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.REQUESTED)
                    },
                ),
            ).willReturn(requestedRequest())

            val result = useCase.issue(UserCouponCommand.Issue(userId = 1L, couponId = 11L))

            assertThat(result.requestId).isEqualTo(101L)
            assertThat(result.couponId).isEqualTo(11L)
            assertThat(result.status).isEqualTo("REQUESTED")
            then(eventPublisher).should().publishEvent(
                check<CouponIssueRequestedEvent> { event ->
                    assertThat(event.requestId).isEqualTo(101L)
                    assertThat(event.couponId).isEqualTo(11L)
                    assertThat(event.userId).isEqualTo(1L)
                },
            )
        }
    }

    @Nested
    @DisplayName("동일 userId + couponId 요청이 이미 있으면")
    inner class WhenIssueRequestExists {
        @Test
        @DisplayName("REQUESTED 요청을 그대로 재사용한다")
        fun issue_requested_reusesExistingRequest() {
            given(couponIssueRequestRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(requestedRequest())

            val result = useCase.issue(UserCouponCommand.Issue(userId = 1L, couponId = 11L))

            assertThat(result.requestId).isEqualTo(101L)
            assertThat(result.status).isEqualTo("REQUESTED")
            then(couponIssueRequestRepository).should(never()).save(check<CouponIssueRequest> { })
        }

        @Test
        @DisplayName("ISSUED 요청을 그대로 재사용한다")
        fun issue_issued_reusesExistingRequest() {
            given(couponIssueRequestRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(issuedRequest())

            val result = useCase.issue(UserCouponCommand.Issue(userId = 1L, couponId = 11L))

            assertThat(result.requestId).isEqualTo(101L)
            assertThat(result.status).isEqualTo("ISSUED")
            assertThat(result.couponId).isEqualTo(11L)
        }

        @Test
        @DisplayName("FAILED 요청을 그대로 재사용한다")
        fun issue_failed_reusesExistingRequest() {
            given(couponIssueRequestRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(failedRequest())

            val result = useCase.issue(UserCouponCommand.Issue(userId = 1L, couponId = 11L))

            assertThat(result.requestId).isEqualTo(101L)
            assertThat(result.status).isEqualTo("FAILED")
        }
    }
}
