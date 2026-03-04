package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        return issuedCouponJpaRepository.save(issuedCoupon)
    }

    override fun findById(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findById(id).orElse(null)
    }

    override fun findByIdWithLock(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByIdWithLock(id)
    }

    override fun findAllByUserId(userId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByUserId(userId)
    }

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByCouponId(couponId, pageable)
    }

    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean {
        return issuedCouponJpaRepository.existsByCouponIdAndUserId(couponId, userId)
    }
}
