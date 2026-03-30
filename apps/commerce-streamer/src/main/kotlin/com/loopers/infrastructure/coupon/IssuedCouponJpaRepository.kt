package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface IssuedCouponJpaRepository : JpaRepository<IssuedCoupon, Long> {
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean

    @Modifying
    @Query(
        "UPDATE issued_coupons SET used_at = NOW() WHERE coupon_id = :couponId AND user_id = :userId AND used_at IS NULL",
        nativeQuery = true,
    )
    fun markUsed(couponId: Long, userId: Long): Int
}
