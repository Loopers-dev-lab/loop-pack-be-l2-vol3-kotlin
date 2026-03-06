package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCouponListUseCase(
    private val issuedCouponRepository: IssuedCouponRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional(readOnly = true)
    fun getList(userId: Long): List<UserCouponResult.ListItem> {
        val issuedCoupons = issuedCouponRepository.findAllByUserId(userId)
        if (issuedCoupons.isEmpty()) return emptyList()

        val couponIds = issuedCoupons.map { it.couponId }.distinct()
        val couponsById = couponIds.mapNotNull { couponRepository.findById(it) }
            .associateBy { it.id!! }

        return issuedCoupons.map { issued ->
            val coupon = couponsById[issued.couponId]
            UserCouponResult.ListItem(
                id = issued.id!!,
                couponId = issued.couponId,
                couponName = coupon?.name ?: "",
                couponType = coupon?.type?.name ?: "",
                discountValue = coupon?.discountValue ?: 0,
                displayStatus = issued.displayStatus().name,
                expiredAt = issued.expiredAt,
                usedAt = issued.usedAt,
            )
        }
    }
}
