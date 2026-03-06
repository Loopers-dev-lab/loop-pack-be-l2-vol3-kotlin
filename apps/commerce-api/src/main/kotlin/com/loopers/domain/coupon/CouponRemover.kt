package com.loopers.domain.coupon

import org.springframework.stereotype.Component

@Component
class CouponRemover(
    private val couponReader: CouponReader,
    private val couponRepository: CouponRepository,
) {

    fun remove(id: Long) {
        couponReader.getById(id)
        couponRepository.deleteById(id)
    }
}
