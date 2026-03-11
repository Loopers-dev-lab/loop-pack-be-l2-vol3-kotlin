package com.loopers.application.coupon

import com.loopers.domain.PageResult
import com.loopers.domain.coupon.repository.CouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponsAdminUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int): PageResult<CouponInfo> {
        return couponRepository.findAllIncludeDeleted(page, size).map { CouponInfo.from(it) }
    }
}
