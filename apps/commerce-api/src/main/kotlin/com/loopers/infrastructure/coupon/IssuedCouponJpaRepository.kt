package com.loopers.infrastructure.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponJpaModel, Long> {
    fun findAllByMemberId(memberId: Long): List<IssuedCouponJpaModel>
    fun findAllByCouponTemplateId(templateId: Long, pageable: Pageable): Page<IssuedCouponJpaModel>
}
