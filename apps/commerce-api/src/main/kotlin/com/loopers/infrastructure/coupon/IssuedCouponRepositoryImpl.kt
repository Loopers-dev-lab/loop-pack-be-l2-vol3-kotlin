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

    override fun findByIdAndDeletedAtIsNull(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId)
    }

    override fun findAllByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId: Long, pageable: Pageable): Page<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId, pageable)
    }
}
