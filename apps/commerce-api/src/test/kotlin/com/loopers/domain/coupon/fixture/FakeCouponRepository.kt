package com.loopers.domain.coupon.fixture

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository

class FakeCouponRepository : CouponRepository {

    private val store = HashMap<Long, Coupon>()
    private var sequence = 1L

    override fun save(coupon: Coupon): Long {
        val id = coupon.persistenceId ?: sequence++
        val persisted = Coupon.reconstitute(
            persistenceId = id,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxIssueCount = coupon.maxIssueCount,
            issuedCount = coupon.issuedCount,
            expiredAt = coupon.expiredAt,
            deletedAt = coupon.deletedAt,
        )
        store[id] = persisted
        return id
    }

    override fun findById(id: Long): Coupon? {
        return store[id]
    }

    override fun incrementIssuedCount(id: Long): Int {
        val coupon = store[id] ?: return 0
        val updated = Coupon.reconstitute(
            persistenceId = coupon.persistenceId!!,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxIssueCount = coupon.maxIssueCount,
            issuedCount = coupon.issuedCount + 1,
            expiredAt = coupon.expiredAt,
            deletedAt = coupon.deletedAt,
        )
        store[id] = updated
        return 1
    }

    override fun findAll(): List<Coupon> {
        return store.values.toList()
    }
}
