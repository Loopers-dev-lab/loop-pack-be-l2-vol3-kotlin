package com.loopers.application.admin.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.dto.CouponTemplateDetailInfo
import com.loopers.domain.coupon.dto.IssuedCouponInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminCouponFacade(
    private val couponService: CouponService,
) {

    fun getCouponTemplates(pageable: Pageable): Page<CouponTemplateDetailInfo> {
        val templatePage = couponService.getActiveTemplates(pageable)
        return templatePage.map { CouponTemplateDetailInfo.from(it) }
    }

    fun getCouponTemplate(templateId: Long): CouponTemplateDetailInfo {
        val template = couponService.getTemplateInfo(templateId)
        return CouponTemplateDetailInfo.from(template)
    }

    @Transactional
    fun createCouponTemplate(
        name: String,
        type: com.loopers.domain.coupon.CouponType,
        value: java.math.BigDecimal,
        minOrderAmount: java.math.BigDecimal,
        expiredAt: java.time.ZonedDateTime,
    ): CouponTemplateDetailInfo {
        val template = couponService.createTemplate(name, type, value, minOrderAmount, expiredAt)
        return CouponTemplateDetailInfo.from(template)
    }

    @Transactional
    fun updateCouponTemplate(
        templateId: Long,
        name: String,
        value: java.math.BigDecimal,
        minOrderAmount: java.math.BigDecimal,
        expiredAt: java.time.ZonedDateTime,
    ): CouponTemplateDetailInfo {
        val template = couponService.updateTemplate(templateId, name, value, minOrderAmount, expiredAt)
        return CouponTemplateDetailInfo.from(template)
    }

    @Transactional
    fun deleteCouponTemplate(templateId: Long) {
        couponService.deleteTemplate(templateId)
    }

    fun getIssuedCoupons(templateId: Long, pageable: Pageable): Page<IssuedCouponInfo> {
        // 템플릿 존재 여부 확인
        couponService.getTemplateInfo(templateId)

        val couponPage = couponService.getIssuedCoupons(templateId, pageable)
        return convertToIssuedCouponInfoPage(couponPage)
    }

    private fun convertToIssuedCouponInfoPage(couponPage: Page<Coupon>): Page<IssuedCouponInfo> {
        val couponInfos = couponPage.content.map { coupon ->
            val template = couponService.getTemplateInfo(coupon.templateId)
            IssuedCouponInfo.from(coupon, template.name)
        }
        return org.springframework.data.domain.PageImpl(couponInfos, couponPage.pageable, couponPage.totalElements)
    }
}
