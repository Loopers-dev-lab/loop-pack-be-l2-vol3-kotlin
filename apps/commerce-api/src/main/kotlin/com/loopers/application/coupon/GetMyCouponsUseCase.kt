package com.loopers.application.coupon

import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.coupon.UserCouponStatus
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMyCouponsUseCase(
    private val userCouponRepository: UserCouponRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, status: UserCouponStatus?, page: Int, size: Int): PageResult<UserCouponInfo> {
        val result = userCouponRepository.findAllByUserId(userId, status, page, size)
        return PageResult.of(
            content = result.content.map { UserCouponInfo.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
    }
}
