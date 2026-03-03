package com.loopers.application.coupon

import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CreateCouponAdminUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var useCase: CreateCouponAdminUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        useCase = CreateCouponAdminUseCase(couponRepository)
    }

    @Nested
    @DisplayName("쿠폰 생성 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 정보로 생성하면 쿠폰이 저장되고 반환된다")
        fun createCoupon_withValidData_savesAndReturnsCoupon() {
            // arrange
            val command = CouponCommand.CreateCoupon(
                name = "신규 가입 쿠폰",
                type = "FIXED",
                value = 1000L,
                maxDiscount = null,
                minOrderAmount = null,
                totalQuantity = 100,
                expiredAt = "2099-12-31T23:59:59+09:00",
            )

            // act
            val result = useCase.execute(command)

            // assert
            assertThat(result.name).isEqualTo("신규 가입 쿠폰")
            assertThat(result.type).isEqualTo("FIXED")
            assertThat(result.value).isEqualTo(1000L)
            assertThat(result.totalQuantity).isEqualTo(100)
        }

        @Test
        @DisplayName("유효하지 않은 type을 입력하면 BAD_REQUEST 예외가 발생한다")
        fun createCoupon_invalidType_throwsBadRequest() {
            // arrange
            val command = CouponCommand.CreateCoupon(
                name = "쿠폰",
                type = "INVALID_TYPE",
                value = 1000L,
                maxDiscount = null,
                minOrderAmount = null,
                totalQuantity = null,
                expiredAt = "2099-12-31T23:59:59+09:00",
            )

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
