package com.loopers.application.coupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.coupon.CouponTemplateModel
import com.loopers.domain.coupon.CouponTemplateRepository
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import java.time.ZonedDateTime
import org.springframework.stereotype.Component

@Component
class CouponService(
    private val couponTemplateRepository: CouponTemplateRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    fun createTemplate(command: CouponCommand.CreateTemplate): CouponTemplateModel {
        val template = CouponTemplateModel(
            name = command.name,
            type = command.type,
            value = command.value,
            minOrderAmount = command.minOrderAmount,
            maxDiscountAmount = command.maxDiscountAmount,
            expirationPolicy = command.expirationPolicy,
            expiredAt = command.expiredAt,
            validDays = command.validDays,
        )
        return couponTemplateRepository.save(template)
    }

    fun getTemplate(id: Long): CouponTemplateModel {
        return couponTemplateRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 템플릿입니다.")
    }

    fun getTemplates(pageQuery: PageQuery): PageResult<CouponTemplateModel> {
        return couponTemplateRepository.findAll(pageQuery)
    }

    fun updateTemplate(id: Long, command: CouponCommand.UpdateTemplate): CouponTemplateModel {
        val template = getTemplate(id)
        if (template.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 템플릿입니다.")
        }
        val updated = template.update(
            name = command.name,
            type = command.type,
            value = command.value,
            minOrderAmount = command.minOrderAmount,
            maxDiscountAmount = command.maxDiscountAmount,
            expirationPolicy = command.expirationPolicy,
            expiredAt = command.expiredAt,
            validDays = command.validDays,
        )
        return couponTemplateRepository.save(updated)
    }

    fun deleteTemplate(id: Long) {
        val template = getTemplate(id)
        if (template.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 템플릿입니다.")
        }
        val deleted = template.delete()
        couponTemplateRepository.save(deleted)
    }

    fun issueCoupon(memberId: Long, templateId: Long): IssuedCouponModel {
        val template = getTemplate(templateId)
        if (!template.isIssuable()) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 불가능한 쿠폰 템플릿입니다.")
        }
        val issuedAt = ZonedDateTime.now()
        val expiredAt = template.calculateExpiredAt(issuedAt)
        val issuedCoupon = IssuedCouponModel(
            couponTemplateId = templateId,
            memberId = memberId,
            expiredAt = expiredAt,
        )
        return issuedCouponRepository.save(issuedCoupon)
    }

    fun getIssuedCoupons(memberId: Long): List<IssuedCouponModel> {
        return issuedCouponRepository.findAllByMemberId(memberId)
    }

    fun getIssuedCouponsByTemplate(templateId: Long, pageQuery: PageQuery): PageResult<IssuedCouponModel> {
        return issuedCouponRepository.findAllByTemplateId(templateId, pageQuery)
    }

    fun getIssuedCouponById(id: Long): IssuedCouponModel {
        return issuedCouponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 발급 쿠폰입니다.")
    }

    fun saveIssuedCoupon(model: IssuedCouponModel): IssuedCouponModel {
        return issuedCouponRepository.save(model)
    }
}
