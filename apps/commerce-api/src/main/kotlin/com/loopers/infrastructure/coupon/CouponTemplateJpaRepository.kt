package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplateModel
import org.springframework.data.jpa.repository.JpaRepository

interface CouponTemplateJpaRepository : JpaRepository<CouponTemplateModel, Long>
