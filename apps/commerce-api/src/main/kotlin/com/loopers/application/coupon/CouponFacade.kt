package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponFacade(
    private val couponService: CouponService,
    private val userService: UserService,
) {
    fun issueCoupon(loginId: String, password: String, couponTemplateId: Long): IssuedCouponInfo {
        val user = getAuthenticatedUser(loginId, password)
        val issuedCoupon = couponService.issueCoupon(user.id, couponTemplateId)
        val template = couponService.getCouponTemplate(couponTemplateId)
        return IssuedCouponInfo.from(issuedCoupon, template)
    }

    fun getUserCoupons(loginId: String, password: String): List<IssuedCouponInfo> {
        val user = getAuthenticatedUser(loginId, password)
        val issuedCoupons = couponService.getUserIssuedCoupons(user.id)
        return issuedCoupons.map { issuedCoupon ->
            val template = couponService.getCouponTemplate(issuedCoupon.couponTemplateId)
            IssuedCouponInfo.from(issuedCoupon, template)
        }
    }

    fun getCouponTemplates(pageable: Pageable): Page<CouponTemplateInfo> {
        return couponService.getCouponTemplates(pageable)
            .map { CouponTemplateInfo.from(it) }
    }

    fun getCouponTemplate(id: Long): CouponTemplateInfo {
        return couponService.getCouponTemplate(id)
            .let { CouponTemplateInfo.from(it) }
    }

    fun createCouponTemplate(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateInfo {
        return couponService.createCouponTemplate(name, type, value, minOrderAmount, expiredAt)
            .let { CouponTemplateInfo.from(it) }
    }

    fun updateCouponTemplate(
        id: Long,
        name: String,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateInfo {
        return couponService.updateCouponTemplate(id, name, value, minOrderAmount, expiredAt)
            .let { CouponTemplateInfo.from(it) }
    }

    fun deleteCouponTemplate(id: Long) {
        couponService.deleteCouponTemplate(id)
    }

    fun getIssuedCoupons(couponTemplateId: Long, pageable: Pageable): Page<IssuedCouponInfo> {
        val template = couponService.getCouponTemplate(couponTemplateId)
        return couponService.getIssuedCouponsByCouponTemplateId(couponTemplateId, pageable)
            .map { IssuedCouponInfo.from(it, template) }
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
}
