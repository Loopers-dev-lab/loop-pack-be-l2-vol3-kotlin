package com.loopers.domain.coupon

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.domain.coupon.repository.IssuedCouponRepository

class FakeIssuedCouponRepository : IssuedCouponRepository {

    private val issuedCoupons = mutableListOf<IssuedCoupon>()
    private var sequence = 1L

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        if (issuedCoupon.id != 0L) {
            issuedCoupons.removeIf { it.id == issuedCoupon.id }
            issuedCoupons.add(issuedCoupon)
            return issuedCoupon
        }
        val saved = IssuedCoupon(
            id = sequence++,
            refCouponId = issuedCoupon.refCouponId,
            refUserId = issuedCoupon.refUserId,
            status = issuedCoupon.status,
            usedAt = issuedCoupon.usedAt,
            createdAt = issuedCoupon.createdAt,
        )
        issuedCoupons.add(saved)
        return saved
    }

    override fun findById(id: Long): IssuedCoupon? {
        return issuedCoupons.find { it.id == id }
    }

    override fun findByIdForUpdate(id: Long): IssuedCoupon? {
        return issuedCoupons.find { it.id == id }
    }

    override fun findByRefCouponIdAndRefUserId(couponId: CouponId, userId: UserId): IssuedCoupon? {
        return issuedCoupons.find { it.refCouponId == couponId && it.refUserId == userId }
    }

    override fun findAllByRefUserId(userId: UserId): List<IssuedCoupon> {
        return issuedCoupons.filter { it.refUserId == userId }
    }

    override fun findAllByRefCouponId(couponId: CouponId, page: Int, size: Int): PageResult<IssuedCoupon> {
        val filtered = issuedCoupons.filter { it.refCouponId == couponId }
        val offset = page * size
        val content = filtered.drop(offset).take(size)
        return PageResult(content, filtered.size.toLong(), page, size)
    }
}
