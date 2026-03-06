package com.loopers.domain.coupon

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository

class FakeCouponRepository : CouponRepository {

    private val coupons = mutableListOf<Coupon>()
    private var sequence = 1L

    override fun save(coupon: Coupon): Coupon {
        if (coupon.id != CouponId(0)) {
            coupons.removeIf { it.id == coupon.id }
            coupons.add(coupon)
            return coupon
        }
        val saved = Coupon(
            id = CouponId(sequence++),
            name = coupon.name,
            type = coupon.type,
            value = coupon.value,
            maxDiscount = coupon.maxDiscount,
            minOrderAmount = coupon.minOrderAmount,
            totalQuantity = coupon.totalQuantity,
            issuedCount = coupon.issuedCount,
            expiredAt = coupon.expiredAt,
            deletedAt = coupon.deletedAt,
        )
        coupons.add(saved)
        return saved
    }

    override fun findById(id: CouponId): Coupon? {
        return coupons.find { it.id == id }
    }

    override fun findByIdForUpdate(id: CouponId): Coupon? {
        return coupons.find { it.id == id }
    }

    override fun findAll(page: Int, size: Int): PageResult<Coupon> {
        val active = coupons.filter { it.deletedAt == null }
        val offset = page * size
        val content = active.drop(offset).take(size)
        return PageResult(content, active.size.toLong(), page, size)
    }

    override fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Coupon> {
        val offset = page * size
        val content = coupons.drop(offset).take(size)
        return PageResult(content, coupons.size.toLong(), page, size)
    }

    override fun findAllByIds(ids: List<CouponId>): List<Coupon> {
        return coupons.filter { it.id in ids }
    }
}
