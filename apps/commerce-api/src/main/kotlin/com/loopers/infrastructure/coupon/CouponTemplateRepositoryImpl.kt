package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplateModel
import com.loopers.domain.coupon.CouponTemplateRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CouponTemplateRepositoryImpl(
    private val jpa: CouponTemplateJpaRepository,
) : CouponTemplateRepository {
    override fun save(couponTemplate: CouponTemplateModel): CouponTemplateModel = jpa.save(couponTemplate)
    override fun findById(id: Long): CouponTemplateModel? = jpa.findById(id).orElse(null)
    override fun findAll(pageable: Pageable): Page<CouponTemplateModel> = jpa.findAll(pageable)
    override fun deleteById(id: Long) = jpa.deleteById(id)
}
