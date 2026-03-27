package com.loopers.application.coupon

import com.loopers.application.event.CouponIssueRequestOutboxAppender
import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueRequestService
import com.loopers.domain.coupon.CouponIssueRequestStatus
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime
import com.loopers.support.error.CoreException

@DisplayName("CouponFacade")
class CouponFacadeTest {

    private val couponService: CouponService = mockk()
    private val couponIssueService: CouponIssueService = mockk()
    private val couponIssueRequestService: CouponIssueRequestService = mockk()
    private val couponIssueRequestOutboxAppender: CouponIssueRequestOutboxAppender = mockk(relaxed = true)
    private val facade = CouponFacade(
        couponService,
        couponIssueService,
        couponIssueRequestService,
        couponIssueRequestOutboxAppender,
    )

    companion object {
        private const val USER_ID = 1L
        private const val COUPON_ID = 10L
    }

    private fun createCoupon(
        id: Long = COUPON_ID,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponModel {
        val coupon = CouponModel(
            name = "테스트 쿠폰",
            type = CouponType.RATE,
            value = 10L,
            expiredAt = expiredAt,
        )
        return spyk(coupon) {
            every { this@spyk.id } returns id
        }
    }

    @DisplayName("issue")
    @Nested
    inner class Issue {
        @DisplayName("쿠폰 발급 요청을 저장하고 ACCEPTED 상태를 반환한다")
        @Test
        fun acceptsIssueRequest() {
            // arrange
            val coupon = createCoupon()
            val request = spyk(CouponIssueRequestModel(couponId = COUPON_ID, userId = USER_ID))
            every { request.id } returns 99L
            every { couponService.findById(COUPON_ID) } returns coupon
            every { couponIssueRequestService.create(COUPON_ID, USER_ID) } returns request

            // act
            val result = facade.issue(COUPON_ID, USER_ID)

            // assert
            assertThat(result.requestId).isEqualTo(99L)
            assertThat(result.couponId).isEqualTo(COUPON_ID)
            assertThat(result.userId).isEqualTo(USER_ID)
            assertThat(result.status).isEqualTo(CouponIssueRequestStatus.ACCEPTED)
            verify(exactly = 1) { couponIssueRequestOutboxAppender.append(request) }
            verify(exactly = 0) { couponIssueService.issue(any(), any()) }
        }
    }

    @DisplayName("findMyCoupons")
    @Nested
    inner class FindMyCoupons {
        @DisplayName("사용자의 쿠폰 목록을 쿠폰 상세 정보와 함께 반환한다")
        @Test
        fun returnsCouponsWithDetails() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val coupon = createCoupon()
            val issues = listOf(CouponIssueModel(couponId = COUPON_ID, userId = USER_ID))
            every { couponIssueService.findByUserId(USER_ID, pageable) } returns PageImpl(issues)
            every { couponService.findAllByIds(listOf(COUPON_ID)) } returns listOf(coupon)

            // act
            val result = facade.findMyCoupons(USER_ID, pageable)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].couponName).isEqualTo("테스트 쿠폰")
        }

        @DisplayName("만료된 쿠폰은 EXPIRED 상태로 반환한다")
        @Test
        fun returnsExpiredStatus_whenCouponExpired() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val expiredCoupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            val issues = listOf(CouponIssueModel(couponId = COUPON_ID, userId = USER_ID))
            every { couponIssueService.findByUserId(USER_ID, pageable) } returns PageImpl(issues)
            every { couponService.findAllByIds(listOf(COUPON_ID)) } returns listOf(expiredCoupon)

            // act
            val result = facade.findMyCoupons(USER_ID, pageable)

            // assert
            assertThat(result.content[0].status).isEqualTo(CouponIssueStatus.EXPIRED)
        }
    }

    @DisplayName("findIssueRequest")
    @Nested
    inner class FindIssueRequest {
        @DisplayName("요청 소유자가 조회하면 상태 정보를 반환한다")
        @Test
        fun returnsIssueRequest() {
            // arrange
            val request = spyk(CouponIssueRequestModel(couponId = COUPON_ID, userId = USER_ID))
            every { request.id } returns 55L
            every { couponIssueRequestService.findById(55L) } returns request

            // act
            val result = facade.findIssueRequest(55L, USER_ID)

            // assert
            assertThat(result.requestId).isEqualTo(55L)
            assertThat(result.status).isEqualTo(CouponIssueRequestStatus.ACCEPTED)
        }

        @DisplayName("요청 소유자가 아니면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsExceptionWhenNotOwner() {
            // arrange
            val request = CouponIssueRequestModel(couponId = COUPON_ID, userId = USER_ID)
            every { couponIssueRequestService.findById(99L) } returns request

            assertThatThrownBy { facade.findIssueRequest(99L, USER_ID + 1) }
                .isInstanceOf(CoreException::class.java)
        }
    }
}
