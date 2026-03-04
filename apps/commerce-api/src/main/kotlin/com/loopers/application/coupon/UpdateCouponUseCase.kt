package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponName
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.product.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateCouponUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun update(id: Long, command: UpdateCouponCommand): CouponInfo {
        val coupon = couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $id")

        val updatedCoupon = coupon.update(
            name = CouponName(command.name),
            discountType = DiscountType.valueOf(command.discountType),
            discountValue = command.discountValue,
            minOrderAmount = Money(command.minOrderAmount),
            maxIssueCount = command.maxIssueCount,
            expiredAt = command.expiredAt,
        )
        couponRepository.save(updatedCoupon)
        return CouponInfo.from(updatedCoupon)
    }
}
