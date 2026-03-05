package com.loopers.domain.coupon

import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.MinOrderAmount
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponChanger(
    private val couponReader: CouponReader,
    private val couponRepository: CouponRepository,
) {

    fun changeInfo(
        id: Long,
        name: String,
        type: CouponType,
        discountValue: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): Coupon {
        val coupon = couponReader.getById(id)
        val couponName = CouponName(name)
        val discount = DiscountValue(discountValue)
        val minAmount = MinOrderAmount(minOrderAmount)

        val changed = coupon.changeInfo(couponName, type, discount, minAmount, expiredAt)
        return couponRepository.save(changed)
    }
}
