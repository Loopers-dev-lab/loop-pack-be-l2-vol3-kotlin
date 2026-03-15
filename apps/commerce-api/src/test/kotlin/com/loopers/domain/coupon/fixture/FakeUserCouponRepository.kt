package com.loopers.domain.coupon.fixture

import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository

class FakeUserCouponRepository : UserCouponRepository {

    private val store = HashMap<Long, UserCoupon>()
    private var sequence = 1L

    override fun save(userCoupon: UserCoupon): Long {
        val id = userCoupon.persistenceId ?: sequence++
        val persisted = UserCoupon.reconstitute(
            persistenceId = id,
            refCouponId = userCoupon.refCouponId,
            refUserId = userCoupon.refUserId,
            status = userCoupon.status,
            discountType = userCoupon.discountType,
            discountValue = userCoupon.discountValue,
            minOrderAmount = userCoupon.minOrderAmount,
            expiredAt = userCoupon.expiredAt,
            usedAt = userCoupon.usedAt,
            issuedAt = userCoupon.issuedAt,
        )
        store[id] = persisted
        return id
    }

    override fun findById(id: Long): UserCoupon? {
        return store[id]
    }

    override fun findByIdForUpdate(id: Long): UserCoupon? {
        return store[id]
    }

    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean {
        return store.values.any { it.refCouponId == couponId && it.refUserId == userId }
    }

    override fun findAllByUserId(userId: Long): List<UserCoupon> {
        return store.values.filter { it.refUserId == userId }
    }

    override fun findAllByCouponId(couponId: Long): List<UserCoupon> {
        return store.values.filter { it.refCouponId == couponId }
    }
}
