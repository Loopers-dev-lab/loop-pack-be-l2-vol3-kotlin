package com.loopers.application.coupon

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.FakeIssuedCouponRepository
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.IssuedCoupon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GetCouponIssuesAdminUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var issuedCouponRepository: FakeIssuedCouponRepository
    private lateinit var useCase: GetCouponIssuesAdminUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        issuedCouponRepository = FakeIssuedCouponRepository()
        useCase = GetCouponIssuesAdminUseCase(issuedCouponRepository)
    }

    @Nested
    @DisplayName("어드민 쿠폰 발급 내역 조회 시")
    inner class Execute {

        @Test
        @DisplayName("특정 쿠폰의 발급 내역을 페이징 조회한다")
        fun getCouponIssues_returnsPaginatedIssuedCoupons() {
            // arrange
            val coupon = couponRepository.save(
                Coupon(
                    name = "테스트 쿠폰",
                    type = Coupon.CouponType.FIXED,
                    value = 1000L,
                    expiredAt = ZonedDateTime.parse("2099-12-31T23:59:59+09:00"),
                ),
            )
            issuedCouponRepository.save(
                IssuedCoupon(
                    refCouponId = coupon.id,
                    refUserId = UserId(1L),
                    createdAt = ZonedDateTime.now(),
                ),
            )
            issuedCouponRepository.save(
                IssuedCoupon(
                    refCouponId = coupon.id,
                    refUserId = UserId(2L),
                    createdAt = ZonedDateTime.now(),
                ),
            )
            // 다른 쿠폰의 발급 내역은 조회되지 않아야 함
            issuedCouponRepository.save(
                IssuedCoupon(
                    refCouponId = 999L,
                    refUserId = UserId(3L),
                    createdAt = ZonedDateTime.now(),
                ),
            )

            // act
            val result = useCase.execute(coupon.id, 0, 10)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2)
        }
    }
}
