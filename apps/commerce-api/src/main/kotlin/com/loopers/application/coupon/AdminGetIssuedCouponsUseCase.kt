package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class AdminGetIssuedCouponsUseCase(
    private val couponService: CouponService,
) : UseCase<ListIssuedCouponsCriteria, ListIssuedCouponsResult> {
    override fun execute(criteria: ListIssuedCouponsCriteria): ListIssuedCouponsResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = couponService.getIssuedCoupons(criteria.couponId, pageable)
        return ListIssuedCouponsResult(
            content = slice.content.map { GetIssuedCouponResult.from(it) },
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
        )
    }
}
