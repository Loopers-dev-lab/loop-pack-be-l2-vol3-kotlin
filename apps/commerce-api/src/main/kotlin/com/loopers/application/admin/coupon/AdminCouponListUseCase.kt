package com.loopers.application.admin.coupon

import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponListUseCase(
    private val couponRepository: CouponRepository,
) {
    @Transactional(readOnly = true)
    fun getList(pageRequest: PageRequest): PageResponse<AdminCouponResult.Summary> {
        return couponRepository.findAll(pageRequest)
            .map { AdminCouponResult.Summary.from(it) }
    }
}
