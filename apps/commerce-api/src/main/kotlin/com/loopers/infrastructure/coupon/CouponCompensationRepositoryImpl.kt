package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CompensationStatus
import com.loopers.domain.coupon.CouponCompensation
import com.loopers.domain.coupon.CouponCompensationRepository
import org.springframework.stereotype.Component

@Component
class CouponCompensationRepositoryImpl(
    private val jpaRepository: CouponCompensationJpaRepository,
) : CouponCompensationRepository {

    override fun save(compensation: CouponCompensation): CouponCompensation {
        return jpaRepository.save(compensation)
    }

    override fun findAllByStatus(status: CompensationStatus): List<CouponCompensation> {
        return jpaRepository.findAllByStatus(status)
    }
}
