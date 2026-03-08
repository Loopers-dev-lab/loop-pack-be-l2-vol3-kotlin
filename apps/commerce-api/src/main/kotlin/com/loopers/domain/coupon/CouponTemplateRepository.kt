package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponTemplateRepository {

    fun findById(id: Long): CouponTemplate?

    fun findAll(pageable: Pageable): Page<CouponTemplate>

    fun findActiveTemplates(pageable: Pageable): Page<CouponTemplate>

    fun save(couponTemplate: CouponTemplate): CouponTemplate
}
