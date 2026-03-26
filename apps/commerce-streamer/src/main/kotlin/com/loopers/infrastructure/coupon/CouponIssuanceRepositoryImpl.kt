package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponIssuance
import com.loopers.domain.coupon.CouponIssuanceRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CouponIssuanceRepositoryImpl(
    private val couponInfoJpaRepository: CouponInfoJpaRepository,
    private val couponIssuanceJpaRepository: CouponIssuanceJpaRepository,
) : CouponIssuanceRepository {

    override fun findCouponById(couponId: Long): CouponInfo? {
        return couponInfoJpaRepository.findByIdOrNull(couponId)
    }

    override fun save(couponIssuance: CouponIssuance): CouponIssuance {
        return couponIssuanceJpaRepository.save(couponIssuance)
    }

    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean {
        return couponIssuanceJpaRepository.existsByCouponIdAndUserId(couponId, userId)
    }

    override fun countByCouponId(couponId: Long): Long {
        return couponIssuanceJpaRepository.countByCouponId(couponId)
    }
}
