package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponTemplateRepository {
    fun save(couponTemplate: CouponTemplateModel): CouponTemplateModel
    fun findById(id: Long): CouponTemplateModel?
    fun findAll(pageable: Pageable): Page<CouponTemplateModel>
    fun deleteById(id: Long)
}
