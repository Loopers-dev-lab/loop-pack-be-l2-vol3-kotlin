package com.loopers.domain.coupon

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CouponIssueRequestService")
class CouponIssueRequestServiceTest {
    private val couponIssueRequestRepository: CouponIssueRequestRepository = mockk()
    private val couponIssueService: CouponIssueService = mockk()
    private val couponIssueRequestService = CouponIssueRequestService(couponIssueRequestRepository, couponIssueService)

    @DisplayName("process")
    @Nested
    inner class Process {
        @DisplayName("발급 성공이면 요청 상태를 COMPLETED로 전환한다")
        @Test
        fun marksCompletedWhenIssued() {
            val request = CouponIssueRequestModel(couponId = 10L, userId = 1L)

            every { couponIssueRequestRepository.findByIdForUpdate(100L) } returns request
            every { couponIssueService.issueFromRequest(10L, 1L) } returns CouponIssueProcessResult.Completed(999L)

            couponIssueRequestService.process(100L)

            assertThat(request.status).isEqualTo(CouponIssueRequestStatus.COMPLETED)
            assertThat(request.couponIssueId).isEqualTo(999L)
        }

        @DisplayName("이미 최종 상태인 요청은 재처리하지 않는다")
        @Test
        fun skipsWhenAlreadyFinalStatus() {
            val request = CouponIssueRequestModel(couponId = 10L, userId = 1L)
            request.markDuplicate()

            every { couponIssueRequestRepository.findByIdForUpdate(200L) } returns request

            couponIssueRequestService.process(200L)

            verify(exactly = 0) { couponIssueService.issueFromRequest(any(), any()) }
        }
    }
}
