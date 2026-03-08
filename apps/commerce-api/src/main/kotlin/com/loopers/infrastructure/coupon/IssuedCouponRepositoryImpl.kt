package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.infrastructure.common.toPageRequest
import com.loopers.infrastructure.common.toPageResult
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        return issuedCouponJpaRepository.save(issuedCoupon)
    }

    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean {
        return issuedCouponJpaRepository.existsByCouponIdAndUserId(couponId, userId)
    }

    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByCouponIdAndUserId(couponId, userId)
    }

    override fun findByUserId(userId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findByUserId(userId)
    }

    override fun findByCouponId(couponId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findByCouponId(couponId)
    }

    override fun findByCouponId(couponId: Long, pageQuery: PageQuery): PageResult<IssuedCoupon> {
        return issuedCouponJpaRepository.findByCouponId(couponId, pageQuery.toPageRequest())
            .toPageResult()
    }

    override fun findByCouponIdAndUserIdWithLock(couponId: Long, userId: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByCouponIdAndUserIdWithLock(couponId, userId)
    }
}
