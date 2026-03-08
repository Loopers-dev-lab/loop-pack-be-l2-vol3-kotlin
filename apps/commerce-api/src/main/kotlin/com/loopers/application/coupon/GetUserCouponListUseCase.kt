package com.loopers.application.coupon

import com.loopers.domain.coupon.UserCouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetUserCouponListUseCase(
    private val userCouponRepository: UserCouponRepository,
) {
    fun getAllByUserId(userId: Long): List<UserCouponInfo> {
        return userCouponRepository.findAllByUserId(userId)
            .map { UserCouponInfo.from(it) }
    }

    fun getAllByCouponId(couponId: Long): List<UserCouponInfo> {
        return userCouponRepository.findAllByCouponId(couponId)
            .map { UserCouponInfo.from(it) }
    }
}
