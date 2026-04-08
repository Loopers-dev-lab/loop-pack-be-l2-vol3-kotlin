package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplate
import org.springframework.data.jpa.repository.JpaRepository

interface CouponTemplateJpaRepository : JpaRepository<CouponTemplate, Long>
