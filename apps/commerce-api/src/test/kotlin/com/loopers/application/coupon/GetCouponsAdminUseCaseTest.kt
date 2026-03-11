package com.loopers.application.coupon

import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.model.Coupon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GetCouponsAdminUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var useCase: GetCouponsAdminUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        useCase = GetCouponsAdminUseCase(couponRepository)
    }

    @Nested
    @DisplayName("어드민 쿠폰 목록 조회 시")
    inner class Execute {

        @Test
        @DisplayName("삭제된 쿠폰도 포함하여 페이징 조회한다")
        fun getCouponsAdmin_includesDeleted() {
            // arrange
            couponRepository.save(
                Coupon(
                    name = "활성 쿠폰",
                    type = Coupon.CouponType.FIXED,
                    value = 1000L,
                    expiredAt = ZonedDateTime.parse("2099-12-31T23:59:59+09:00"),
                ),
            )
            val deleted = couponRepository.save(
                Coupon(
                    name = "삭제된 쿠폰",
                    type = Coupon.CouponType.RATE,
                    value = 10L,
                    expiredAt = ZonedDateTime.parse("2099-12-31T23:59:59+09:00"),
                ),
            )
            deleted.delete()
            couponRepository.save(deleted)

            // act
            val result = useCase.execute(0, 10)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2)
        }
    }
}
