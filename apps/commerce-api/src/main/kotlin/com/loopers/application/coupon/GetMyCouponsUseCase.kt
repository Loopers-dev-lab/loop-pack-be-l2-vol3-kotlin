package com.loopers.application.coupon

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMyCouponsUseCase(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    private val log = LoggerFactory.getLogger(GetMyCouponsUseCase::class.java)

    @Transactional(readOnly = true)
    fun execute(userId: Long): List<MyCouponInfo> {
        val issuedCoupons = issuedCouponRepository.findAllByRefUserId(UserId(userId))
        val couponIds = issuedCoupons.map { it.refCouponId }.distinct()
        val coupons = couponRepository.findAllByIds(couponIds).associateBy { it.id }
        return issuedCoupons.mapNotNull { issuedCoupon ->
            val coupon = coupons[issuedCoupon.refCouponId]
            if (coupon == null) {
                log.warn("쿠폰 템플릿을 찾을 수 없습니다. issuedCouponId={}, refCouponId={}", issuedCoupon.id, issuedCoupon.refCouponId)
                return@mapNotNull null
            }
            MyCouponInfo.from(issuedCoupon, coupon)
        }
    }
}
