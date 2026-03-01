package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        return issuedCouponJpaRepository.save(issuedCoupon)
    }

    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean {
        return issuedCouponJpaRepository.existsByCouponIdAndUserId(couponId, userId)
    }

    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByCouponIdAndUserId(couponId, userId)
    }

    override fun findByUserId(userId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findByUserId(userId)
    }

    override fun findByCouponId(couponId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findByCouponId(couponId)
    }
}
