package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class AdminGetCouponsUseCase(
    private val couponService: CouponService,
) : UseCase<ListCouponsCriteria, ListCouponsResult> {
    override fun execute(criteria: ListCouponsCriteria): ListCouponsResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = couponService.getCoupons(pageable)
        return ListCouponsResult(
            content = slice.content.map { GetCouponResult.from(it) },
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
        )
    }
}
