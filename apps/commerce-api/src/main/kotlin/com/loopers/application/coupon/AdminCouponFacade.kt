package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class AdminCouponFacade(
    private val couponService: CouponService,
) {

    @Transactional
    fun createCoupon(
        name: String,
        discountType: DiscountType,
        discountValue: Long,
        totalQuantity: Int,
        expiresAt: ZonedDateTime,
    ): CouponInfo {
        return couponService.create(
            name = name,
            discount = Discount(discountType, discountValue),
            quantity = CouponQuantity(totalQuantity, 0),
            expiresAt = expiresAt,
        ).let { CouponInfo.from(it) }
    }

    fun getCoupons(pageQuery: PageQuery): PageResult<CouponInfo> {
        return couponService.findAll(pageQuery)
            .map { CouponInfo.from(it) }
    }

    fun getCoupon(couponId: Long): CouponInfo {
        return couponService.findCouponById(couponId)
            .let { CouponInfo.from(it) }
    }
}
