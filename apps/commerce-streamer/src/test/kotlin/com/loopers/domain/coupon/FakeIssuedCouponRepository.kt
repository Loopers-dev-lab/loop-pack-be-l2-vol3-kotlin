package com.loopers.domain.coupon

import com.loopers.domain.coupon.repository.IssuedCouponRepository

class FakeIssuedCouponRepository : IssuedCouponRepository {

    data class Entry(val refCouponId: Long, val refUserId: Long)

    private val store = mutableListOf<Entry>()

    override fun existsByRefCouponIdAndRefUserId(couponId: Long, userId: Long): Boolean {
        return store.any { it.refCouponId == couponId && it.refUserId == userId }
    }

    override fun save(refCouponId: Long, refUserId: Long) {
        store.add(Entry(refCouponId, refUserId))
    }

    fun count(): Int = store.size
}
