package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun save(coupon: Coupon): Coupon {
        return couponJpaRepository.save(coupon)
    }

    override fun findById(id: Long): Coupon? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdWithLock(id: Long): Coupon? {
        return couponJpaRepository.findByIdWithLockAndDeletedAtIsNull(id)
    }

    override fun findByIdIn(ids: List<Long>): List<Coupon> {
        return couponJpaRepository.findByIdInAndDeletedAtIsNull(ids)
    }
}
