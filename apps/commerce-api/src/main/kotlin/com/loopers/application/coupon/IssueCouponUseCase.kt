package com.loopers.application.coupon

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class IssueCouponUseCase(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    @Transactional
    fun execute(userId: Long, couponId: Long): IssuedCouponInfo {
        val couponIdVo = CouponId(couponId)
        val coupon = couponRepository.findByIdForUpdate(couponIdVo)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")

        if (coupon.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
        }

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }

        val userIdVo = UserId(userId)
        val existing = issuedCouponRepository.findByRefCouponIdAndRefUserId(couponIdVo, userIdVo)
        if (existing != null) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 발급받은 쿠폰입니다.")
        }

        coupon.issue()

        val issuedCoupon = IssuedCoupon(
            refCouponId = coupon.id,
            refUserId = userIdVo,
            createdAt = ZonedDateTime.now(),
        )
        val savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon)
        couponRepository.save(coupon)

        return IssuedCouponInfo.from(savedIssuedCoupon)
    }
}
