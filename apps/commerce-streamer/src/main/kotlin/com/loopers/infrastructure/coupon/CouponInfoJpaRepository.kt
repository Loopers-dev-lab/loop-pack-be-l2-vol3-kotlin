package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponInfo
import org.springframework.data.jpa.repository.JpaRepository

interface CouponInfoJpaRepository : JpaRepository<CouponInfo, Long>
