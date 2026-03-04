package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun save(coupon: Coupon): Coupon {
        return couponJpaRepository.save(coupon)
    }

    override fun findById(id: Long): Coupon? {
        return couponJpaRepository.findById(id).orElse(null)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): Coupon? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAll(pageable: Pageable): Page<Coupon> {
        return couponJpaRepository.findAllByDeletedAtIsNull(pageable)
    }
}
