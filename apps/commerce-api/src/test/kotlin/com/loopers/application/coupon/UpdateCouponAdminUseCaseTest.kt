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

class UpdateCouponAdminUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var useCase: UpdateCouponAdminUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        useCase = UpdateCouponAdminUseCase(couponRepository)
    }

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        type: Coupon.CouponType = Coupon.CouponType.FIXED,
        value: Long = 1000L,
        expiredAt: ZonedDateTime = ZonedDateTime.parse("2099-12-31T23:59:59+09:00"),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                type = type,
                value = value,
                expiredAt = expiredAt,
            ),
        )
    }

    @Nested
    @DisplayName("쿠폰 수정 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 정보로 수정하면 쿠폰이 변경된다")
        fun updateCoupon_withValidData_updatesCoupon() {
            // arrange
            val coupon = createCoupon()
            val command = CouponCommand.UpdateCoupon(
                name = "수정된 쿠폰",
                type = "FIXED",
                value = 2000L,
                maxDiscount = null,
                minOrderAmount = null,
                totalQuantity = null,
                expiredAt = "2099-12-31T23:59:59+09:00",
            )

            // act
            val result = useCase.execute(coupon.id, command)

            // assert
            assertThat(result.name).isEqualTo("수정된 쿠폰")
            assertThat(result.value).isEqualTo(2000L)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 수정하면 NOT_FOUND 예외가 발생한다")
        fun updateCoupon_nonExistent_throwsNotFound() {
            // arrange
            val command = CouponCommand.UpdateCoupon(
                name = "수정된 쿠폰",
                type = null,
                value = null,
                maxDiscount = null,
                minOrderAmount = null,
                totalQuantity = null,
                expiredAt = null,
            )

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 쿠폰을 수정하면 NOT_FOUND 예외가 발생한다")
        fun updateCoupon_deletedCoupon_throwsNotFound() {
            // arrange
            val coupon = createCoupon()
            coupon.delete()
            couponRepository.save(coupon)
            val command = CouponCommand.UpdateCoupon(
                name = "수정된 쿠폰",
                type = null,
                value = null,
                maxDiscount = null,
                minOrderAmount = null,
                totalQuantity = null,
                expiredAt = null,
            )

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(coupon.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
