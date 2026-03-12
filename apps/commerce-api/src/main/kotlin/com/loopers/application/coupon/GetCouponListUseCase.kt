package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetCouponListUseCase(
    private val couponRepository: CouponRepository,
) {

    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int): PageResult<CouponInfo> {
        val result = couponRepository.findAllActive(page, size)
        return PageResult.of(
            content = result.content.map { CouponInfo.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
    }
}
