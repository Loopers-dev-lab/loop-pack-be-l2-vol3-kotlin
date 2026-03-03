package com.loopers.application.coupon

import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.model.Coupon
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class GetCouponAdminUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var useCase: GetCouponAdminUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        useCase = GetCouponAdminUseCase(couponRepository)
    }

    private fun createCoupon(name: String = "테스트 쿠폰"): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                expiredAt = ZonedDateTime.parse("2099-12-31T23:59:59+09:00"),
            ),
        )
    }

    @Nested
    @DisplayName("어드민 쿠폰 상세 조회 시")
    inner class Execute {

        @Test
        @DisplayName("쿠폰 ID로 조회하면 쿠폰 정보가 반환된다")
        fun getCouponAdmin_withValidId_returnsCouponInfo() {
            // arrange
            val coupon = createCoupon("신규 가입 쿠폰")

            // act
            val result = useCase.execute(coupon.id)

            // assert
            assertThat(result.id).isEqualTo(coupon.id)
            assertThat(result.name).isEqualTo("신규 가입 쿠폰")
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 조회하면 NOT_FOUND 예외가 발생한다")
        fun getCouponAdmin_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
