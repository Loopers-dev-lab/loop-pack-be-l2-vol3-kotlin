package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CompensationStatus
import com.loopers.domain.coupon.CouponCompensation
import org.springframework.data.jpa.repository.JpaRepository

interface CouponCompensationJpaRepository : JpaRepository<CouponCompensation, Long> {
    fun findAllByStatus(status: CompensationStatus): List<CouponCompensation>
}
