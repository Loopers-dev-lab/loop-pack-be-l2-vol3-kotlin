package com.loopers.domain.coupon.repository

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.IssuedCoupon

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon
    fun findById(id: Long): IssuedCoupon?
    fun findByIdForUpdate(id: Long): IssuedCoupon?
    fun findByRefCouponIdAndRefUserId(couponId: CouponId, userId: UserId): IssuedCoupon?
    fun findAllByRefUserId(userId: UserId): List<IssuedCoupon>
    fun findAllByRefCouponId(couponId: CouponId, page: Int, size: Int): PageResult<IssuedCoupon>
}
