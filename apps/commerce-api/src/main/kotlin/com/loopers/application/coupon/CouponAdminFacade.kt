package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponAdminFacade(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
) {
    fun create(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponAdminInfo {
        val coupon = CouponModel(
            name = name,
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
        return CouponAdminInfo.from(couponService.create(coupon))
    }

    fun findById(id: Long): CouponAdminInfo {
        return CouponAdminInfo.from(couponService.findById(id))
    }

    fun findAll(pageable: Pageable): Page<CouponAdminInfo> {
        return couponService.findAll(pageable).map { CouponAdminInfo.from(it) }
    }

    fun update(
        id: Long,
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponAdminInfo {
        val coupon = couponService.findById(id)
        coupon.update(name, type, value, minOrderAmount, expiredAt)
        return CouponAdminInfo.from(couponService.create(coupon))
    }

    fun delete(id: Long) {
        couponService.delete(id)
    }

    fun findIssuesByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssueAdminInfo> {
        return couponIssueService.findAllByCouponId(couponId, pageable)
            .map { CouponIssueAdminInfo.from(it) }
    }
}
