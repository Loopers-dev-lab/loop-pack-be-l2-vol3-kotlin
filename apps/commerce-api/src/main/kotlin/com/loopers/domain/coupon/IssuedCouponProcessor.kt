package com.loopers.domain.coupon

import org.springframework.stereotype.Component

@Component
class IssuedCouponProcessor(
    private val couponReader: CouponReader,
    private val issuedCouponReader: IssuedCouponReader,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    fun reserve(issuedCouponId: Long, memberId: Long, totalPrice: Long): Reservation {
        val issuedCoupon = issuedCouponReader.getByIdForUpdate(issuedCouponId)
        issuedCoupon.validateOwner(memberId)

        val coupon = couponReader.getById(issuedCoupon.couponId)
        coupon.validateApplicable(totalPrice)

        issuedCoupon.reserve()
        val savedCoupon = issuedCouponRepository.save(issuedCoupon)

        return Reservation(
            issuedCouponId = requireNotNull(savedCoupon.id),
            discountAmount = coupon.calculateDiscount(totalPrice),
        )
    }

    fun confirmUseIfReserved(issuedCouponId: Long) {
        val issuedCoupon = issuedCouponReader.getByIdForUpdate(issuedCouponId)
        if (issuedCoupon.status != CouponStatus.RESERVED) {
            return
        }

        issuedCoupon.confirmUse()
        issuedCouponRepository.save(issuedCoupon)
    }

    fun releaseIfReserved(issuedCouponId: Long) {
        val issuedCoupon = issuedCouponReader.getByIdForUpdate(issuedCouponId)
        if (issuedCoupon.status != CouponStatus.RESERVED) {
            return
        }

        issuedCoupon.release()
        issuedCouponRepository.save(issuedCoupon)
    }

    data class Reservation(
        val issuedCouponId: Long,
        val discountAmount: Long,
    )
}
