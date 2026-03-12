package com.loopers.domain.coupon

interface CouponTemplateRepository {
    fun save(template: CouponTemplate): CouponTemplate
    fun findById(id: Long): CouponTemplate?
    fun findAll(): List<CouponTemplate>
    fun deleteById(id: Long)
}
