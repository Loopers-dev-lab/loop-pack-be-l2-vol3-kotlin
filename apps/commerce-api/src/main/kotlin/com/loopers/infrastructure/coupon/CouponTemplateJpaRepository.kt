package com.loopers.infrastructure.coupon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CouponTemplateJpaRepository : JpaRepository<CouponTemplateEntity, Long> {

    @Query("SELECT ct FROM CouponTemplateEntity ct WHERE ct.deletedAt IS NULL")
    fun findAllActive(): List<CouponTemplateEntity>
}
