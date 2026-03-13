package com.loopers.application.coupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminCouponFacade(
    private val couponService: CouponService,
) {
    @Transactional
    fun createTemplate(command: CouponCommand.CreateTemplate): CouponTemplateInfo {
        val template = couponService.createTemplate(command)
        return CouponTemplateInfo.from(template)
    }

    @Transactional(readOnly = true)
    fun getTemplate(id: Long): CouponTemplateInfo {
        val template = couponService.getTemplate(id)
        return CouponTemplateInfo.from(template)
    }

    @Transactional(readOnly = true)
    fun getTemplates(page: Int, size: Int): PageResult<CouponTemplateInfo> {
        val result = couponService.getTemplates(PageQuery(page, size))
        return PageResult(
            content = result.content.map { CouponTemplateInfo.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional
    fun updateTemplate(id: Long, command: CouponCommand.UpdateTemplate): CouponTemplateInfo {
        val template = couponService.updateTemplate(id, command)
        return CouponTemplateInfo.from(template)
    }

    @Transactional
    fun deleteTemplate(id: Long) {
        couponService.deleteTemplate(id)
    }

    @Transactional(readOnly = true)
    fun getIssuedCouponsByTemplate(templateId: Long, page: Int, size: Int): PageResult<IssuedCouponInfo> {
        val template = couponService.getTemplate(templateId)
        val result = couponService.getIssuedCouponsByTemplate(templateId, PageQuery(page, size))
        return PageResult(
            content = result.content.map { IssuedCouponInfo.from(it, template) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
