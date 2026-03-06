package com.loopers.application.coupon

import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.model.Coupon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class DeleteCouponAdminUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var useCase: DeleteCouponAdminUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        useCase = DeleteCouponAdminUseCase(couponRepository)
    }

    private fun createCoupon(): Coupon {
        return couponRepository.save(
            Coupon(
                name = "테스트 쿠폰",
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                expiredAt = ZonedDateTime.parse("2099-12-31T23:59:59+09:00"),
            ),
        )
    }

    @Nested
    @DisplayName("쿠폰 삭제 시")
    inner class Execute {

        @Test
        @DisplayName("쿠폰을 삭제하면 soft delete된다")
        fun deleteCoupon_softDeletes() {
            // arrange
            val coupon = createCoupon()

            // act
            useCase.execute(coupon.id.value)

            // assert
            val deleted = couponRepository.findById(coupon.id)
            assertThat(deleted?.isDeleted()).isTrue()
        }

        @Test
        @DisplayName("이미 삭제된 쿠폰을 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteCoupon_alreadyDeleted_isIdempotent() {
            // arrange
            val coupon = createCoupon()
            coupon.delete()
            couponRepository.save(coupon)

            // act & assert — 예외 없이 정상 반환
            useCase.execute(coupon.id.value)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteCoupon_nonExistent_isIdempotent() {
            // act & assert — 예외 없이 정상 반환
            useCase.execute(999L)
        }
    }
}
