package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssuer
import com.loopers.domain.coupon.CouponReader
import com.loopers.domain.coupon.IssuedCouponReader
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponUseCase(
    private val couponIssuer: CouponIssuer,
    private val couponReader: CouponReader,
    private val issuedCouponReader: IssuedCouponReader,
) {

    @Transactional
    fun issueCoupon(couponId: Long, memberId: Long): CouponInfo.IssuedDetail {
        val issuedCoupon = couponIssuer.issue(couponId, memberId)
        val coupon = couponReader.getById(issuedCoupon.couponId)
        return CouponInfo.IssuedDetail.from(issuedCoupon, coupon)
    }

    @Transactional(readOnly = true)
    fun getMyCoupons(memberId: Long): List<CouponInfo.IssuedDetail> {
        val issuedCoupons = issuedCouponReader.getAllByMemberId(memberId)
        if (issuedCoupons.isEmpty()) return emptyList()

        val couponIds = issuedCoupons.map { it.couponId }.distinct()
        val couponMap = couponReader.getAllByIds(couponIds).associateBy { it.id }

        return issuedCoupons.map { issuedCoupon ->
            val coupon = couponMap[issuedCoupon.couponId]
                ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)
            CouponInfo.IssuedDetail.from(issuedCoupon, coupon)
        }
    }
}
