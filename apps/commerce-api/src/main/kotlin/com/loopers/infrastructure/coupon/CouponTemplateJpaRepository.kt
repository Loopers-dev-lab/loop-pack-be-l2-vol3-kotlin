package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponTemplateJpaRepository : JpaRepository<CouponTemplate, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponTemplate?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponTemplate>
}
