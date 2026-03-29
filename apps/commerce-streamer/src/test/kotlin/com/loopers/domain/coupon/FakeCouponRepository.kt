package com.loopers.domain.coupon

import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository

class FakeCouponRepository : CouponRepository {

    private val store = mutableListOf<Coupon>()
    private var sequence = 1L

    override fun findById(id: Long): Coupon? {
        return store.find { it.id == id }
    }

    override fun findByIdForUpdate(id: Long): Coupon? {
        return store.find { it.id == id }
    }

    override fun save(coupon: Coupon): Coupon {
        if (coupon.id != 0L) {
            store.removeIf { it.id == coupon.id }
            store.add(coupon)
            return coupon
        }
        setId(coupon, sequence++)
        store.add(coupon)
        return coupon
    }

    private fun setId(entity: Coupon, id: Long) {
        Coupon::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
