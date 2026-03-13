package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponTemplateRepository {
    fun save(couponTemplate: CouponTemplate): CouponTemplate
    fun findByIdAndDeletedAtIsNull(id: Long): CouponTemplate?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponTemplate>
}
