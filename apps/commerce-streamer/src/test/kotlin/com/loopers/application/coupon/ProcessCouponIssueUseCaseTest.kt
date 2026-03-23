package com.loopers.application.coupon

import com.loopers.domain.coupon.FakeCouponIssueRequestRepository
import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.FakeIssuedCouponRepository
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.model.CouponIssueRequest.CouponIssueStatus
import com.loopers.domain.event.FakeEventHandledRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class ProcessCouponIssueUseCaseTest {

    private lateinit var couponIssueRequestRepository: FakeCouponIssueRequestRepository
    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var issuedCouponRepository: FakeIssuedCouponRepository
    private lateinit var eventHandledRepository: FakeEventHandledRepository
    private lateinit var useCase: ProcessCouponIssueUseCase

    @BeforeEach
    fun setUp() {
        couponIssueRequestRepository = FakeCouponIssueRequestRepository()
        couponRepository = FakeCouponRepository()
        issuedCouponRepository = FakeIssuedCouponRepository()
        eventHandledRepository = FakeEventHandledRepository()
        useCase = ProcessCouponIssueUseCase(
            couponIssueRequestRepository,
            couponRepository,
            issuedCouponRepository,
            eventHandledRepository,
        )
    }

    private fun createCoupon(totalQuantity: Int? = 100, issuedCount: Int = 0): Coupon {
        return couponRepository.save(
            Coupon(
                totalQuantity = totalQuantity,
                issuedCount = issuedCount,
                expiredAt = ZonedDateTime.now().plusDays(7),
                deletedAt = null,
            ),
        )
    }

    private fun createRequest(couponId: Long, userId: Long): CouponIssueRequest {
        return couponIssueRequestRepository.save(
            CouponIssueRequest(
                requestId = UUID.randomUUID().toString(),
                couponId = couponId,
                userId = userId,
            ),
        )
    }

    @Nested
    @DisplayName("P-1: 정상 발급")
    inner class Success {

        @Test
        fun `수량 잔여분이 있으면 쿠폰이 발급되고 SUCCESS로 저장된다`() {
            val coupon = createCoupon(totalQuantity = 10, issuedCount = 0)
            val request = createRequest(coupon.id, userId = 1L)

            useCase.execute(eventId = request.requestId, couponId = coupon.id, userId = 1L)

            val updated = couponIssueRequestRepository.findByRequestId(request.requestId)!!
            assertThat(updated.status).isEqualTo(CouponIssueStatus.SUCCESS)
            assertThat(issuedCouponRepository.count()).isEqualTo(1)
            assertThat(couponRepository.findById(coupon.id)!!.issuedCount).isEqualTo(1)
        }

        @Test
        fun `이미 처리된 eventId는 중복 처리하지 않는다`() {
            val coupon = createCoupon()
            val request = createRequest(coupon.id, userId = 1L)

            useCase.execute(eventId = request.requestId, couponId = coupon.id, userId = 1L)
            useCase.execute(eventId = request.requestId, couponId = coupon.id, userId = 1L)

            assertThat(issuedCouponRepository.count()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("P-2: 수량 소진")
    inner class SoldOut {

        @Test
        fun `잔여 수량이 0이면 SOLD_OUT으로 저장된다`() {
            val coupon = createCoupon(totalQuantity = 10, issuedCount = 10)
            val request = createRequest(coupon.id, userId = 1L)

            useCase.execute(eventId = request.requestId, couponId = coupon.id, userId = 1L)

            val updated = couponIssueRequestRepository.findByRequestId(request.requestId)!!
            assertThat(updated.status).isEqualTo(CouponIssueStatus.SOLD_OUT)
            assertThat(issuedCouponRepository.count()).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("P-3: 중복 발급 방지")
    inner class Duplicate {

        @Test
        fun `동일 userId와 couponId로 중복 소비 시 DUPLICATE로 저장된다`() {
            val coupon = createCoupon()
            issuedCouponRepository.save(refCouponId = coupon.id, refUserId = 1L)
            val request = createRequest(coupon.id, userId = 1L)

            useCase.execute(eventId = request.requestId, couponId = coupon.id, userId = 1L)

            val updated = couponIssueRequestRepository.findByRequestId(request.requestId)!!
            assertThat(updated.status).isEqualTo(CouponIssueStatus.DUPLICATE)
        }
    }
}
