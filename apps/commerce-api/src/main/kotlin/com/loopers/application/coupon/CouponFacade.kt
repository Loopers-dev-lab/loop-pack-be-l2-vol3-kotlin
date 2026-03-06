package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponTemplateService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.user.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponFacade(
    private val couponTemplateService: CouponTemplateService,
    private val userCouponService: UserCouponService,
    private val userService: UserService,
) {
    @Transactional
    fun issueCoupon(loginId: String, couponTemplateId: Long): UserCouponInfo {
        val user = userService.getUserByLoginId(loginId)
        return userCouponService.issueCoupon(user.id, couponTemplateId)
            .let { UserCouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getTemplates(pageable: Pageable): Page<CouponTemplateInfo> =
        couponTemplateService.getTemplates(pageable).map { CouponTemplateInfo.from(it) }

    @Transactional(readOnly = true)
    fun getTemplateById(id: Long): CouponTemplateInfo =
        couponTemplateService.getTemplateById(id).let { CouponTemplateInfo.from(it) }

    @Transactional
    fun createTemplate(
        name: String,
        type: String,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateInfo =
        couponTemplateService.createTemplate(
            name = name,
            type = CouponType.valueOf(type),
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        ).let { CouponTemplateInfo.from(it) }

    @Transactional
    fun updateTemplate(
        id: Long,
        name: String,
        type: String,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateInfo =
        couponTemplateService.updateTemplate(
            id = id,
            name = name,
            type = CouponType.valueOf(type),
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        ).let { CouponTemplateInfo.from(it) }

    @Transactional
    fun deleteTemplate(id: Long) {
        couponTemplateService.deleteTemplate(id)
    }

    @Transactional(readOnly = true)
    fun getIssuesByCouponTemplateId(couponTemplateId: Long, pageable: Pageable): Page<UserCouponInfo> =
        userCouponService.getIssuesByCouponTemplateId(couponTemplateId, pageable)
            .map { UserCouponInfo.from(it) }
}
