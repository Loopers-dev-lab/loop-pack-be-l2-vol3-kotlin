package com.loopers.application.coupon

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.FakeCouponIssueRequestRepository
import com.loopers.domain.coupon.model.CouponIssueRequest
import java.util.UUID
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetCouponIssueStatusUseCaseTest {

    private lateinit var couponIssueRequestRepository: FakeCouponIssueRequestRepository
    private lateinit var useCase: GetCouponIssueStatusUseCase

    @BeforeEach
    fun setUp() {
        couponIssueRequestRepository = FakeCouponIssueRequestRepository()
        useCase = GetCouponIssueStatusUseCase(couponIssueRequestRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        fun `PENDING 상태의 요청을 조회하면 상태가 반환된다`() {
            val request = couponIssueRequestRepository.save(
                CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = CouponId(1L), userId = UserId(1L)),
            )

            val result = useCase.execute(request.requestId, 1L)

            assertThat(result.requestId).isEqualTo(request.requestId)
            assertThat(result.status).isEqualTo("PENDING")
        }

        @Test
        fun `SUCCESS 상태의 요청을 조회하면 상태가 반환된다`() {
            val request = couponIssueRequestRepository.save(
                CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = CouponId(1L), userId = UserId(1L)),
            )
            request.markSuccess()
            couponIssueRequestRepository.save(request)

            val result = useCase.execute(request.requestId, 1L)

            assertThat(result.status).isEqualTo("SUCCESS")
        }

        @Test
        fun `존재하지 않는 requestId로 조회하면 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                useCase.execute("nonexistent-request-id", 1L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        fun `다른 사용자의 requestId로 조회하면 NOT_FOUND 예외가 발생한다`() {
            val request = couponIssueRequestRepository.save(
                CouponIssueRequest(requestId = UUID.randomUUID().toString(), couponId = CouponId(1L), userId = UserId(1L)),
            )

            val exception = assertThrows<CoreException> {
                useCase.execute(request.requestId, 999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
