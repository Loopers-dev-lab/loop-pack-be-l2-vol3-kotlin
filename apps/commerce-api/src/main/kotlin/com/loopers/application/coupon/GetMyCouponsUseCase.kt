package com.loopers.application.coupon

import com.loopers.domain.coupon.UserCouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyCouponsUseCase(
    private val userCouponRepository: UserCouponRepository,
) {
    fun getMyAll(userId: Long): List<UserCouponInfo> {
        return userCouponRepository.findAllByUserId(userId)
            .map { UserCouponInfo.from(it) }
    }
}
