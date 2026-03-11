package com.loopers.domain.coupon

import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.MinOrderAmount
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponRegister(
    private val couponRepository: CouponRepository,
) {

    fun register(
        name: String,
        type: CouponType,
        discountValue: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): Coupon {
        val couponName = CouponName(name)
        val discount = DiscountValue(discountValue)
        val minAmount = MinOrderAmount(minOrderAmount)

        val coupon = Coupon(
            name = couponName,
            type = type,
            discountValue = discount,
            minOrderAmount = minAmount,
            expiredAt = expiredAt,
        )

        return couponRepository.save(coupon)
    }
}
