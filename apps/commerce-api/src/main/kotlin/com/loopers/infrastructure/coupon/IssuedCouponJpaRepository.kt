package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface IssuedCouponJpaRepository : JpaRepository<IssuedCoupon, Long> {
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon?
    fun findByUserId(userId: Long): List<IssuedCoupon>
    fun findByCouponId(couponId: Long): List<IssuedCoupon>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ic FROM IssuedCoupon ic WHERE ic.couponId = :couponId AND ic.userId = :userId")
    fun findByCouponIdAndUserIdWithLock(couponId: Long, userId: Long): IssuedCoupon?
}
