package com.loopers.infrastructure.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponTemplateJpaRepository : JpaRepository<CouponTemplateJpaModel, Long> {
    fun findAllByOrderByIdDesc(pageable: Pageable): Page<CouponTemplateJpaModel>
}
