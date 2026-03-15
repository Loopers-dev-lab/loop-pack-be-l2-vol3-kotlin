package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UserCouponMapperTest {

    @Test
    fun `UserCouponEntity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = UserCouponEntity(
            id = null,
            couponId = 1L,
            userId = 1L,
            status = CouponStatus.AVAILABLE,
            discountType = DiscountType.FIXED,
            discountValue = 1000L,
            minOrderAmount = 10000L,
            expiredAt = ZonedDateTime.now().plusDays(30),
            usedAt = null,
            issuedAt = ZonedDateTime.now(),
        )

        assertThatThrownBy { UserCouponMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("UserCouponEntity.id가 null입니다")
    }
}
