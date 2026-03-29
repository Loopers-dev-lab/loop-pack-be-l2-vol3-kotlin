package com.loopers.domain.coupon

interface CouponCompensationRepository {
    fun save(compensation: CouponCompensation): CouponCompensation
    fun findAllByStatus(status: CompensationStatus): List<CouponCompensation>
}
