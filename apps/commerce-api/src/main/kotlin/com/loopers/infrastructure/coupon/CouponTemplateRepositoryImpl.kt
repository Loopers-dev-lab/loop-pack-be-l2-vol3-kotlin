package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponTemplateRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponTemplateRepositoryImpl(
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
) : CouponTemplateRepository {
    override fun save(couponTemplate: CouponTemplate): CouponTemplate {
        return couponTemplateJpaRepository.save(couponTemplate)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): CouponTemplate? {
        return couponTemplateJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponTemplate> {
        return couponTemplateJpaRepository.findAllByDeletedAtIsNull(pageable)
    }
}
