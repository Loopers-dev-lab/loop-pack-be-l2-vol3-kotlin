package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@DisplayName("CouponAdminFacade")
class CouponAdminFacadeTest {

    private val couponService: CouponService = mockk()
    private val couponIssueService: CouponIssueService = mockk()
    private val facade = CouponAdminFacade(couponService, couponIssueService)

    private val expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30)

    @DisplayName("create")
    @Nested
    inner class Create {
        @DisplayName("쿠폰 템플릿을 생성하고 CouponAdminInfo를 반환한다")
        @Test
        fun createsCouponTemplate() {
            // arrange
            every { couponService.create(any()) } answers { firstArg() }

            // act
            val result = facade.create(
                name = "테스트 쿠폰",
                type = CouponType.RATE,
                value = 10L,
                minOrderAmount = 10000L,
                expiredAt = expiredAt,
            )

            // assert
            assertThat(result.name).isEqualTo("테스트 쿠폰")
            assertThat(result.type).isEqualTo(CouponType.RATE)
            verify(exactly = 1) { couponService.create(any()) }
        }
    }

    @DisplayName("findIssuesByCouponId")
    @Nested
    inner class FindIssues {
        @DisplayName("쿠폰별 발급 내역을 반환한다")
        @Test
        fun returnsIssues() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val issues = listOf(CouponIssueModel(couponId = 1L, userId = 1L))
            every { couponIssueService.findAllByCouponId(1L, pageable) } returns PageImpl(issues)

            // act
            val result = facade.findIssuesByCouponId(1L, pageable)

            // assert
            assertThat(result.content).hasSize(1)
        }
    }
}
