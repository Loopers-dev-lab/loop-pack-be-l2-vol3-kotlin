package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponType
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class DeleteCouponUseCaseTest @Autowired constructor(
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val deleteCouponUseCase: DeleteCouponUseCase,
    private val couponRepository: CouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerCoupon(): CouponInfo {
        return registerCouponUseCase.execute(
            CouponCommand.Register(
                name = "테스트 쿠폰",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    @DisplayName("쿠폰 삭제")
    @Nested
    inner class Execute {

        @DisplayName("삭제하면 soft delete 된다")
        @Test
        fun success() {
            val coupon = registerCoupon()

            deleteCouponUseCase.execute(coupon.id)

            val deleted = couponRepository.findByIdOrNull(coupon.id)
            assertThat(deleted?.isDeleted()).isTrue()
        }

        @DisplayName("존재하지 않는 쿠폰 삭제 시 COUPON_NOT_FOUND 에러가 발생한다")
        @Test
        fun failWhenNotFound() {
            val exception = assertThrows<CoreException> {
                deleteCouponUseCase.execute(999L)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)
        }

        @DisplayName("이미 삭제된 쿠폰 삭제 시 COUPON_NOT_FOUND 에러가 발생한다")
        @Test
        fun failWhenAlreadyDeleted() {
            val coupon = registerCoupon()
            deleteCouponUseCase.execute(coupon.id)

            val exception = assertThrows<CoreException> {
                deleteCouponUseCase.execute(coupon.id)
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)
        }
    }
}
