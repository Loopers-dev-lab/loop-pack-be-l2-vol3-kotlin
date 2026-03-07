package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.user.UserService
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class AdminCouponFacade(
    private val couponService: CouponService,
    private val userService: UserService,
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

    @Transactional
    fun updateCoupon(
        couponId: Long,
        name: String,
        discountType: DiscountType,
        discountValue: Long,
        expiresAt: ZonedDateTime,
    ): CouponInfo {
        return couponService.update(
            couponId = couponId,
            name = name,
            discount = Discount(discountType, discountValue),
            expiresAt = expiresAt,
        ).let { CouponInfo.from(it) }
    }

    @Transactional
    fun deleteCoupon(couponId: Long) {
        couponService.delete(couponId)
    }

    @Transactional(readOnly = true)
    fun getCoupons(pageQuery: PageQuery): PageResult<CouponInfo> {
        return couponService.findAll(pageQuery)
            .map { CouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getCoupon(couponId: Long): CouponInfo {
        return couponService.findCouponById(couponId)
            .let { CouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getCouponIssues(couponId: Long, pageQuery: PageQuery): PageResult<CouponIssueInfo> {
        val coupon = couponService.findCouponById(couponId)
        val issuedCouponsPage = couponService.findIssuedCouponsByCouponId(couponId, pageQuery)
        val userIds = issuedCouponsPage.content.map { it.userId }
        val usersMap = userService.findByIds(userIds).associateBy { it.id }

        return issuedCouponsPage.map { issuedCoupon ->
            CouponIssueInfo.from(
                issuedCoupon = issuedCoupon,
                user = usersMap[issuedCoupon.userId],
                couponExpiresAt = coupon.expiresAt,
            )
        }
    }
}
