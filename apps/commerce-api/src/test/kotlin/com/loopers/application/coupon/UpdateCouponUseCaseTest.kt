package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponType
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class UpdateCouponUseCaseTest @Autowired constructor(
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val updateCouponUseCase: UpdateCouponUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerCoupon(): CouponInfo {
        return registerCouponUseCase.execute(
            CouponCommand.Register(
                name = "원래 쿠폰",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    @DisplayName("쿠폰 수정")
    @Nested
    inner class Execute {

        @DisplayName("정상 수정 시 성공한다")
        @Test
        fun success() {
            val coupon = registerCoupon()
            val newExpiredAt = ZonedDateTime.now().plusDays(60)

            val result = updateCouponUseCase.execute(
                CouponCommand.Update(
                    couponId = coupon.id,
                    name = "수정된 쿠폰",
                    value = 2000,
                    minOrderAmount = 5000,
                    expiredAt = newExpiredAt,
                ),
            )

            assertAll(
                { assertThat(result.name).isEqualTo("수정된 쿠폰") },
                { assertThat(result.value).isEqualTo(2000) },
                { assertThat(result.minOrderAmount).isEqualTo(5000) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 수정 시 COUPON_NOT_FOUND 에러가 발생한다")
        @Test
        fun failWhenNotFound() {
            val exception = assertThrows<CoreException> {
                updateCouponUseCase.execute(
                    CouponCommand.Update(
                        couponId = 999L,
                        name = "수정",
                        value = 1000,
                        minOrderAmount = null,
                        expiredAt = ZonedDateTime.now().plusDays(30),
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)
        }
    }
}
