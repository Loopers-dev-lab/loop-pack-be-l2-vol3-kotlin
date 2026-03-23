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

    @Nested
    @DisplayName("P-4: 존재하지 않는 쿠폰")
    inner class CouponNotFound {

        @Test
        fun `쿠폰이 존재하지 않으면 FAILED로 저장된다`() {
            val nonExistentCouponId = 999L
            val request = createRequest(nonExistentCouponId, userId = 1L)

            useCase.execute(eventId = request.requestId, couponId = nonExistentCouponId, userId = 1L)

            val updated = couponIssueRequestRepository.findByRequestId(request.requestId)!!
            assertThat(updated.status).isEqualTo(CouponIssueStatus.FAILED)
            assertThat(issuedCouponRepository.count()).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Q-1: 선착순 수량 초과 방지 — 단일 파티션 순차 처리 검증")
    inner class ConcurrencyControl {

        @Test
        fun `총 수량 5개 쿠폰에 10명이 순차 요청하면 정확히 5명만 SUCCESS이다`() {
            val totalQuantity = 5
            val requestCount = 10
            val coupon = createCoupon(totalQuantity = totalQuantity, issuedCount = 0)
            val requests = (1L..requestCount).map { userId ->
                createRequest(coupon.id, userId = userId)
            }

            // 단일 파티션 Kafka → 순차 처리 시뮬레이션
            requests.forEachIndexed { index, request ->
                useCase.execute(
                    eventId = request.requestId,
                    couponId = coupon.id,
                    userId = (index + 1).toLong(),
                )
            }

            val statuses = requests.map {
                couponIssueRequestRepository.findByRequestId(it.requestId)!!.status
            }
            assertThat(statuses.count { it == CouponIssueStatus.SUCCESS }).isEqualTo(totalQuantity)
            assertThat(statuses.count { it == CouponIssueStatus.SOLD_OUT }).isEqualTo(requestCount - totalQuantity)
            assertThat(couponRepository.findById(coupon.id)!!.issuedCount).isEqualTo(totalQuantity)
            assertThat(issuedCouponRepository.count()).isEqualTo(totalQuantity)
        }

        @Test
        fun `순차 처리 시 선착순으로 발급되고 나머지는 SOLD_OUT이다`() {
            val totalQuantity = 3
            val requestCount = 5
            val coupon = createCoupon(totalQuantity = totalQuantity, issuedCount = 0)
            val requests = (1L..requestCount).map { userId ->
                createRequest(coupon.id, userId = userId)
            }

            requests.forEachIndexed { index, request ->
                useCase.execute(
                    eventId = request.requestId,
                    couponId = coupon.id,
                    userId = (index + 1).toLong(),
                )
            }

            // 처음 3명은 SUCCESS, 나머지 2명은 SOLD_OUT (FIFO 순서 보장)
            val statuses = requests.map {
                couponIssueRequestRepository.findByRequestId(it.requestId)!!.status
            }
            statuses.take(totalQuantity).forEach { status ->
                assertThat(status).isEqualTo(CouponIssueStatus.SUCCESS)
            }
            statuses.drop(totalQuantity).forEach { status ->
                assertThat(status).isEqualTo(CouponIssueStatus.SOLD_OUT)
            }
        }

        @Test
        fun `중복 사용자가 섞인 대량 요청에서도 수량과 중복 제어가 정확하다`() {
            val totalQuantity = 3
            val coupon = createCoupon(totalQuantity = totalQuantity, issuedCount = 0)

            // 5명의 사용자 중 userId=1, userId=2가 각각 2회 요청 (총 7건)
            data class Req(val userId: Long)

            val requestSpecs = listOf(
                Req(1L),
                Req(2L),
                Req(3L),
                Req(1L),
                Req(4L),
                Req(2L),
                Req(5L),
            )
            val requests = requestSpecs.map { createRequest(coupon.id, userId = it.userId) }

            requests.forEachIndexed { index, request ->
                useCase.execute(
                    eventId = request.requestId,
                    couponId = coupon.id,
                    userId = requestSpecs[index].userId,
                )
            }

            val statuses = requests.map {
                couponIssueRequestRepository.findByRequestId(it.requestId)!!.status
            }
            // userId=1 SUCCESS, userId=2 SUCCESS, userId=3 SUCCESS
            assertThat(statuses[0]).isEqualTo(CouponIssueStatus.SUCCESS)
            assertThat(statuses[1]).isEqualTo(CouponIssueStatus.SUCCESS)
            assertThat(statuses[2]).isEqualTo(CouponIssueStatus.SUCCESS)
            // userId=1 재요청 → DUPLICATE (이미 발급됨)
            assertThat(statuses[3]).isEqualTo(CouponIssueStatus.DUPLICATE)
            // userId=4 → SOLD_OUT (수량 소진)
            assertThat(statuses[4]).isEqualTo(CouponIssueStatus.SOLD_OUT)
            // userId=2 재요청 → DUPLICATE
            assertThat(statuses[5]).isEqualTo(CouponIssueStatus.DUPLICATE)
            // userId=5 → SOLD_OUT
            assertThat(statuses[6]).isEqualTo(CouponIssueStatus.SOLD_OUT)

            assertThat(couponRepository.findById(coupon.id)!!.issuedCount).isEqualTo(totalQuantity)
            assertThat(issuedCouponRepository.count()).isEqualTo(totalQuantity)
        }
    }
}
