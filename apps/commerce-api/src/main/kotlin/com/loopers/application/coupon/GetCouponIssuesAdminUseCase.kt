package com.loopers.application.coupon

import com.loopers.domain.PageResult
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponIssuesAdminUseCase(
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    @Transactional(readOnly = true)
    fun execute(couponId: Long, page: Int, size: Int): PageResult<IssuedCouponInfo> {
        return issuedCouponRepository.findAllByRefCouponId(couponId, page, size).map { IssuedCouponInfo.from(it) }
    }
}
