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

    override fun findById(id: Long): CouponTemplate? = couponTemplateJpaRepository.findById(id)
        .orElse(null)

    override fun findAll(pageable: Pageable): Page<CouponTemplate> =
        couponTemplateJpaRepository.findAll(pageable)

    override fun findActiveTemplates(pageable: Pageable): Page<CouponTemplate> =
        couponTemplateJpaRepository.findActiveTemplates(pageable)

    override fun save(couponTemplate: CouponTemplate): CouponTemplate =
        couponTemplateJpaRepository.save(couponTemplate)
}
