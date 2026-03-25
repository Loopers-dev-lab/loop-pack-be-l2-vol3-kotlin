package com.loopers.domain.coupon

import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.model.CouponIssueRequest.CouponIssueStatus
import com.loopers.domain.coupon.repository.CouponIssueRequestRepository
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class CouponIssueRequestTest {

    @Nested
    @DisplayName("CouponIssueRequest 생성")
    inner class Create {

        @Test
        fun `유효한 필드로 생성하면 상태는 PENDING이다`() {
            val requestId = UUID.randomUUID().toString()
            val request = CouponIssueRequest(requestId = requestId, couponId = 1L, userId = 1L)

            assertThat(request.requestId).isEqualTo(requestId)
            assertThat(request.couponId).isEqualTo(1L)
            assertThat(request.userId).isEqualTo(1L)
            assertThat(request.status).isEqualTo(CouponIssueStatus.PENDING)
        }

        @Test
        fun `requestId가 blank이면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponIssueRequest(requestId = "   ", couponId = 1L, userId = 1L)
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `requestId가 빈 문자열이면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponIssueRequest(requestId = "", couponId = 1L, userId = 1L)
            }.isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("상태 변경")
    inner class StatusTransition {

        @Test
        fun `PENDING 상태에서 SUCCESS로 변경된다`() {
            val request = CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = 1L, userId = 1L)
            request.markSuccess()
            assertThat(request.status).isEqualTo(CouponIssueStatus.SUCCESS)
        }

        @Test
        fun `PENDING 상태에서 SOLD_OUT으로 변경된다`() {
            val request = CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = 1L, userId = 1L)
            request.markSoldOut()
            assertThat(request.status).isEqualTo(CouponIssueStatus.SOLD_OUT)
        }

        @Test
        fun `PENDING 상태에서 DUPLICATE로 변경된다`() {
            val request = CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = 1L, userId = 1L)
            request.markDuplicate()
            assertThat(request.status).isEqualTo(CouponIssueStatus.DUPLICATE)
        }

        @Test
        fun `PENDING이 아닌 상태에서 변경하면 예외가 발생한다`() {
            val request = CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = 1L, userId = 1L)
            request.markSuccess()

            assertThatThrownBy { request.markSoldOut() }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("CouponIssueRequestRepository")
    inner class RepositoryTest {

        private lateinit var repository: CouponIssueRequestRepository

        @BeforeEach
        fun setUp() {
            repository = FakeCouponIssueRequestRepository()
        }

        @Test
        fun `저장 후 requestId로 조회된다`() {
            val request = repository.save(CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = 1L, userId = 1L))

            val found = repository.findByRequestId(request.requestId)

            assertThat(found).isNotNull
            assertThat(found!!.id).isEqualTo(request.id)
            assertThat(found.couponId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 requestId로 조회하면 null을 반환한다`() {
            val found = repository.findByRequestId("nonexistent")
            assertThat(found).isNull()
        }
    }
}
