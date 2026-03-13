package com.loopers.application.coupon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponFacade(
    private val couponService: CouponService,
) {
    @Transactional
    fun issueCoupon(memberId: Long, templateId: Long): IssuedCouponInfo {
        val issuedCoupon = couponService.issueCoupon(memberId, templateId)
        val template = couponService.getTemplate(issuedCoupon.couponTemplateId)
        return IssuedCouponInfo.from(issuedCoupon, template)
    }

    @Transactional(readOnly = true)
    fun getMyIssuedCoupons(memberId: Long): List<IssuedCouponInfo> {
        val issuedCoupons = couponService.getIssuedCoupons(memberId)
        if (issuedCoupons.isEmpty()) return emptyList()

        val templateIds = issuedCoupons.map { it.couponTemplateId }.distinct()
        val templateMap = templateIds.associateWith { id ->
            couponService.getTemplate(id)
        }

        return issuedCoupons.map { issued ->
            IssuedCouponInfo.from(issued, templateMap[issued.couponTemplateId]!!)
        }
    }
}
