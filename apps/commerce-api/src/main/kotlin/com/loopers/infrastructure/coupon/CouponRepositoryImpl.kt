package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun findByIdOrNull(id: Long): Coupon? {
        return couponJpaRepository.findById(id).orElse(null)
    }

    override fun findActiveByIdOrNull(id: Long): Coupon? {
        return couponJpaRepository.findActiveByIdOrNull(id)
    }

    override fun findAllActive(page: Int, size: Int): PageResult<Coupon> {
        val pageable = PageRequest.of(page, size)
        val result = couponJpaRepository.findAllActive(pageable)
        return PageResult.of(
            content = result.content,
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }

    override fun save(coupon: Coupon): Coupon {
        return couponJpaRepository.save(coupon)
    }
}
