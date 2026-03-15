package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.DiscountType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class CouponMapperTest {

    @Test
    fun `CouponEntity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = CouponEntity(
            id = null,
            name = "테스트쿠폰",
            discountType = DiscountType.FIXED,
            discountValue = 1000L,
            minOrderAmount = 10000L,
            maxIssueCount = 100,
            issuedCount = 0,
            expiredAt = ZonedDateTime.now().plusDays(30),
            deletedAt = null,
        )

        assertThatThrownBy { CouponMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("CouponEntity.id가 null입니다")
    }
}
