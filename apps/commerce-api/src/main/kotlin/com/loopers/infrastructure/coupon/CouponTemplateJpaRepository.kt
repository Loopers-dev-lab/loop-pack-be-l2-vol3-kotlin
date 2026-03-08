package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CouponTemplateJpaRepository : JpaRepository<CouponTemplate, Long> {

    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.deletedAt IS NULL ORDER BY ct.createdAt DESC")
    fun findActiveTemplates(pageable: Pageable): Page<CouponTemplate>
}
