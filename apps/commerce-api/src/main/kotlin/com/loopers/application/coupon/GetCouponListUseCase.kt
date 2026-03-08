package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCouponListUseCase(
    private val couponRepository: CouponRepository,
) {
    fun getAll(): List<CouponInfo> {
        return couponRepository.findAll().map { CouponInfo.from(it) }
    }
}
