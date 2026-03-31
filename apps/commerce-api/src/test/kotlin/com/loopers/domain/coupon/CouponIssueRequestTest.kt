package com.loopers.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CouponIssueRequest 도메인")
class CouponIssueRequestTest {
    @Nested
    @DisplayName("새 요청을 만들면")
    inner class WhenRequestCreated {
        @Test
        @DisplayName("REQUESTED 상태로 생성된다")
        fun request_success() {
            val request = CouponIssueRequest.request(
                couponId = 11L,
                userId = 1L,
            )

            assertThat(request.id).isNull()
            assertThat(request.couponId).isEqualTo(11L)
            assertThat(request.userId).isEqualTo(1L)
            assertThat(request.status).isEqualTo(CouponIssueRequest.Status.REQUESTED)
            assertThat(request.failureReasonCode).isNull()
            assertThat(request.issuedCouponId).isNull()
        }
    }

    @Nested
    @DisplayName("REQUESTED 요청은 상태 전이할 수 있다")
    inner class WhenTransitioningFromRequested {
        @Test
        @DisplayName("markIssued하면 ISSUED와 issuedCouponId를 기록한다")
        fun markIssued_success() {
            val request = CouponIssueRequest.retrieve(
                id = 101L,
                couponId = 11L,
                userId = 1L,
                status = CouponIssueRequest.Status.REQUESTED,
                failureReasonCode = null,
                issuedCouponId = null,
            )

            val issued = request.markIssued(555L)

            assertThat(issued.status).isEqualTo(CouponIssueRequest.Status.ISSUED)
            assertThat(issued.issuedCouponId).isEqualTo(555L)
            assertThat(issued.failureReasonCode).isNull()
        }

        @Test
        @DisplayName("markFailed하면 FAILED와 failureReasonCode를 기록한다")
        fun markFailed_success() {
            val request = CouponIssueRequest.retrieve(
                id = 101L,
                couponId = 11L,
                userId = 1L,
                status = CouponIssueRequest.Status.REQUESTED,
                failureReasonCode = null,
                issuedCouponId = null,
            )

            val failed = request.markFailed("COUPON_SOLD_OUT")

            assertThat(failed.status).isEqualTo(CouponIssueRequest.Status.FAILED)
            assertThat(failed.failureReasonCode).isEqualTo("COUPON_SOLD_OUT")
            assertThat(failed.issuedCouponId).isNull()
        }
    }
}
