package com.loopers.domain.coupon

interface UserCouponRepository {
    fun save(userCoupon: UserCoupon): Long
    fun findById(id: Long): UserCoupon?
    fun findByIdForUpdate(id: Long): UserCoupon?
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun findAllByUserId(userId: Long): List<UserCoupon>
    fun findAllByCouponId(couponId: Long): List<UserCoupon>
}
