package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.infrastructure.outbox.CouponIssueRequestedOutboxMessagePayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.ZonedDateTime

@DisplayName("CouponIssueRequestedEventHandler")
class CouponIssueRequestedEventHandlerTest {
    private val couponRepository: CouponRepository = mock()
    private val couponIssueRequestRepository: CouponIssueRequestRepository = mock()
    private val issuedCouponRepository: IssuedCouponRepository = mock()
    private val handler = CouponIssueRequestedEventHandler(
        couponRepository = couponRepository,
        couponIssueRequestRepository = couponIssueRequestRepository,
        issuedCouponRepository = issuedCouponRepository,
    )

    private fun payload(
        requestId: Long = 101L,
        couponId: Long = 11L,
        userId: Long = 1L,
    ): CouponIssueRequestedOutboxMessagePayload = CouponIssueRequestedOutboxMessagePayload(
        requestId = requestId,
        couponId = couponId,
        userId = userId,
    )

    private fun request(
        id: Long = 101L,
        couponId: Long = 11L,
        userId: Long = 1L,
        status: CouponIssueRequest.Status = CouponIssueRequest.Status.REQUESTED,
        failureReasonCode: String? = null,
        issuedCouponId: Long? = null,
    ): CouponIssueRequest = CouponIssueRequest.retrieve(
        id = id,
        couponId = couponId,
        userId = userId,
        status = status,
        failureReasonCode = failureReasonCode,
        issuedCouponId = issuedCouponId,
    )

    private fun coupon(
        id: Long = 11L,
        issueLimit: Long? = 3L,
        issuedCount: Long = 0L,
        deletedAt: ZonedDateTime? = null,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon = Coupon.retrieve(
        id = id,
        name = "테스트 쿠폰",
        type = Coupon.Type.FIXED,
        discountValue = 1000L,
        minOrderAmount = null,
        expiredAt = expiredAt,
        issueLimit = issueLimit,
        issuedCount = issuedCount,
        deletedAt = deletedAt,
        createdAt = ZonedDateTime.now(),
    )

    @Nested
    @DisplayName("정상 요청이면")
    inner class WhenSuccess {
        @Test
        @DisplayName("쿠폰을 발급하고 request를 ISSUED로 바꾼다")
        fun handle_success() {
            given(couponIssueRequestRepository.findByIdForUpdate(101L)).willReturn(request())
            given(issuedCouponRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(null)
            given(couponRepository.findByIdForUpdate(11L)).willReturn(coupon())
            given(
                issuedCouponRepository.save(
                    check { issuedCoupon ->
                        assertThat(issuedCoupon.couponId).isEqualTo(11L)
                        assertThat(issuedCoupon.userId).isEqualTo(1L)
                    },
                ),
            ).willReturn(
                IssuedCoupon.retrieve(
                    id = 555L,
                    couponId = 11L,
                    userId = 1L,
                    status = IssuedCoupon.Status.AVAILABLE,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                    usedAt = null,
                    version = 0L,
                ),
            )
            given(
                couponRepository.save(
                    check {
                        assertThat(it.issuedCount).isEqualTo(1L)
                    },
                ),
            ).willReturn(coupon(issuedCount = 1L))
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.ISSUED)
                        assertThat(request.issuedCouponId).isEqualTo(555L)
                    },
                ),
            ).willReturn(request(status = CouponIssueRequest.Status.ISSUED, issuedCouponId = 555L))

            handler.handle(payload())

            verify(couponIssueRequestRepository).save(
                check { request ->
                    assertThat(request.status).isEqualTo(CouponIssueRequest.Status.ISSUED)
                    assertThat(request.issuedCouponId).isEqualTo(555L)
                },
            )
        }
    }

    @Nested
    @DisplayName("쿠폰이 없으면")
    inner class WhenCouponMissing {
        @Test
        @DisplayName("request를 FAILED(COUPON_NOT_FOUND)로 마감한다")
        fun handle_couponNotFound() {
            given(couponIssueRequestRepository.findByIdForUpdate(101L)).willReturn(request())
            given(couponRepository.findByIdForUpdate(11L)).willReturn(null)
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.FAILED)
                        assertThat(request.failureReasonCode).isEqualTo("COUPON_NOT_FOUND")
                    },
                ),
            ).willReturn(request(status = CouponIssueRequest.Status.FAILED, failureReasonCode = "COUPON_NOT_FOUND"))

            handler.handle(payload())

            verify(issuedCouponRepository, never()).save(check<IssuedCoupon> { })
        }
    }

    @Nested
    @DisplayName("쿠폰이 삭제되었으면")
    inner class WhenCouponDeleted {
        @Test
        @DisplayName("request를 FAILED(COUPON_DELETED)로 마감한다")
        fun handle_couponDeleted() {
            given(couponIssueRequestRepository.findByIdForUpdate(101L)).willReturn(request())
            given(couponRepository.findByIdForUpdate(11L)).willReturn(coupon(deletedAt = ZonedDateTime.now()))
            given(issuedCouponRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(null)
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.FAILED)
                        assertThat(request.failureReasonCode).isEqualTo("COUPON_DELETED")
                    },
                ),
            ).willReturn(request(status = CouponIssueRequest.Status.FAILED, failureReasonCode = "COUPON_DELETED"))

            handler.handle(payload())

            verify(issuedCouponRepository, never()).save(check<IssuedCoupon> { })
        }
    }

    @Nested
    @DisplayName("쿠폰이 만료되었으면")
    inner class WhenCouponExpired {
        @Test
        @DisplayName("request를 FAILED(COUPON_EXPIRED)로 마감한다")
        fun handle_couponExpired() {
            given(couponIssueRequestRepository.findByIdForUpdate(101L)).willReturn(request())
            given(couponRepository.findByIdForUpdate(11L)).willReturn(
                coupon(expiredAt = ZonedDateTime.now().minusDays(1)),
            )
            given(issuedCouponRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(null)
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.FAILED)
                        assertThat(request.failureReasonCode).isEqualTo("COUPON_EXPIRED")
                    },
                ),
            ).willReturn(request(status = CouponIssueRequest.Status.FAILED, failureReasonCode = "COUPON_EXPIRED"))

            handler.handle(payload())

            verify(issuedCouponRepository, never()).save(check<IssuedCoupon> { })
        }
    }

    @Nested
    @DisplayName("쿠폰이 소진되면")
    inner class WhenSoldOut {
        @Test
        @DisplayName("request를 FAILED(COUPON_SOLD_OUT)로 마감한다")
        fun handle_soldOut() {
            given(couponIssueRequestRepository.findByIdForUpdate(101L)).willReturn(request())
            given(couponRepository.findByIdForUpdate(11L)).willReturn(coupon(issueLimit = 1L, issuedCount = 1L))
            given(issuedCouponRepository.findByCouponIdAndUserId(11L, 1L)).willReturn(null)
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.FAILED)
                        assertThat(request.failureReasonCode).isEqualTo("COUPON_SOLD_OUT")
                    },
                ),
            ).willReturn(request(status = CouponIssueRequest.Status.FAILED, failureReasonCode = "COUPON_SOLD_OUT"))

            handler.handle(payload())

            verify(issuedCouponRepository, never()).save(check<IssuedCoupon> { })
        }
    }

    @Nested
    @DisplayName("이미 발급된 쿠폰이 있으면")
    inner class WhenAlreadyIssued {
        @Test
        @DisplayName("쿠폰이 소진되어도 기존 issuedCouponId로 request를 정규화한다")
        fun handle_normalizesIssued() {
            given(couponIssueRequestRepository.findByIdForUpdate(101L)).willReturn(request())
            given(couponRepository.findByIdForUpdate(11L)).willReturn(coupon(issueLimit = 1L, issuedCount = 1L))
            given(
                issuedCouponRepository.findByCouponIdAndUserId(11L, 1L),
            ).willReturn(
                IssuedCoupon.retrieve(
                    id = 555L,
                    couponId = 11L,
                    userId = 1L,
                    status = IssuedCoupon.Status.AVAILABLE,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                    usedAt = null,
                    version = 0L,
                ),
            )
            given(
                couponIssueRequestRepository.save(
                    check { request ->
                        assertThat(request.status).isEqualTo(CouponIssueRequest.Status.ISSUED)
                        assertThat(request.issuedCouponId).isEqualTo(555L)
                    },
                ),
            ).willReturn(request(status = CouponIssueRequest.Status.ISSUED, issuedCouponId = 555L))

            handler.handle(payload())

            verify(couponRepository).findByIdForUpdate(11L)
            verify(issuedCouponRepository, never()).save(check<IssuedCoupon> { })
        }
    }
}
