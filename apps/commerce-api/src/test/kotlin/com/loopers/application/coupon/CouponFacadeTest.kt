package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@DisplayName("CouponFacade")
class CouponFacadeTest {

    private val couponService: CouponService = mockk()
    private val couponIssueService: CouponIssueService = mockk()
    private val facade = CouponFacade(couponService, couponIssueService)

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
        @DisplayName("쿠폰을 발급하고 CouponIssueInfo를 반환한다")
        @Test
        fun issuesCoupon() {
            // arrange
            val coupon = createCoupon()
            val issue = CouponIssueModel(couponId = COUPON_ID, userId = USER_ID)
            every { couponIssueService.issue(COUPON_ID, USER_ID) } returns issue
            every { couponService.findById(COUPON_ID) } returns coupon

            // act
            val result = facade.issue(COUPON_ID, USER_ID)

            // assert
            assertThat(result.couponId).isEqualTo(COUPON_ID)
            assertThat(result.status).isEqualTo(CouponIssueStatus.AVAILABLE)
            assertThat(result.couponName).isEqualTo("테스트 쿠폰")
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
}
