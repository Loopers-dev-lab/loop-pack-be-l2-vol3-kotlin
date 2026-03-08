package com.loopers.application.api.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.dto.CouponInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CouponFacade(
    private val couponService: CouponService,
) {

    @Transactional
    fun issueCoupon(userId: Long, templateId: Long): CouponInfo {
        val coupon = couponService.issueCoupon(userId, templateId)
        val template = couponService.getTemplateInfo(coupon.templateId)
        return CouponInfo.from(
            coupon = coupon,
            templateName = template.name,
            type = template.type,
            value = template.value,
            minOrderAmount = template.minOrderAmount,
            expiredAt = template.expiredAt,
        )
    }

    fun getMyCoupons(userId: Long, pageable: Pageable): Page<CouponInfo> {
        val couponPage = couponService.getMyCoupons(userId, pageable)
        return convertToCouponInfoPage(couponPage)
    }

    private fun convertToCouponInfoPage(couponPage: Page<Coupon>): Page<CouponInfo> {
        val couponInfos = couponPage.content.map { coupon ->
            val template = couponService.getTemplateInfo(coupon.templateId)
            CouponInfo.from(
                coupon = coupon,
                templateName = template.name,
                type = template.type,
                value = template.value,
                minOrderAmount = template.minOrderAmount,
                expiredAt = template.expiredAt,
            )
        }
        return PageImpl(couponInfos, couponPage.pageable, couponPage.totalElements)
    }
}
