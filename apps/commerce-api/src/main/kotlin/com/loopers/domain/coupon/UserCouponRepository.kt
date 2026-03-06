package com.loopers.domain.coupon

import com.loopers.support.PageResult

interface UserCouponRepository {
    fun findByIdOrNull(id: Long): UserCoupon?
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon?
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean
    fun findAllByUserId(userId: Long, status: UserCouponStatus?, page: Int, size: Int): PageResult<UserCoupon>
    fun findAllByCouponId(couponId: Long, page: Int, size: Int): PageResult<UserCoupon>
    fun save(userCoupon: UserCoupon): UserCoupon
    fun flush()
}
