package com.loopers.application.coupon

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateCouponAdminUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun execute(couponId: Long, command: CouponCommand.UpdateCoupon): CouponInfo {
        val coupon = couponRepository.findById(CouponId(couponId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        if (coupon.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        }

        val newType = command.type?.let {
            Coupon.CouponType.entries.find { e -> e.name == it }
                ?: throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 쿠폰 타입입니다: $it")
        } ?: coupon.type

        val updated = coupon.update(
            name = command.name ?: coupon.name,
            type = newType,
            value = command.value ?: coupon.value,
            maxDiscount = if (command.maxDiscount != null) Money(command.maxDiscount) else coupon.maxDiscount,
            minOrderAmount = if (command.minOrderAmount != null) Money(command.minOrderAmount) else coupon.minOrderAmount,
            totalQuantity = command.totalQuantity ?: coupon.totalQuantity,
            expiredAt = command.expiredAt ?: coupon.expiredAt,
        )

        val saved = couponRepository.save(updated)
        return CouponInfo.from(saved)
    }
}
