package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.coupon.UserCouponStatus
import com.loopers.support.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class UserCouponRepositoryImpl(
    private val userCouponJpaRepository: UserCouponJpaRepository,
) : UserCouponRepository {

    override fun findByIdOrNull(id: Long): UserCoupon? {
        return userCouponJpaRepository.findById(id).orElse(null)
    }

    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon? {
        return userCouponJpaRepository.findByUserIdAndCouponId(userId, couponId)
    }

    override fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean {
        return userCouponJpaRepository.existsByUserIdAndCouponId(userId, couponId)
    }

    override fun findAllByUserId(userId: Long, status: UserCouponStatus?, page: Int, size: Int): PageResult<UserCoupon> {
        val pageable = PageRequest.of(page, size)
        val result = userCouponJpaRepository.findAllByUserId(userId, status, pageable)
        return PageResult.of(
            content = result.content,
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }

    override fun findAllByCouponId(couponId: Long, page: Int, size: Int): PageResult<UserCoupon> {
        val pageable = PageRequest.of(page, size)
        val result = userCouponJpaRepository.findAllByCouponId(couponId, pageable)
        return PageResult.of(
            content = result.content,
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }

    override fun save(userCoupon: UserCoupon): UserCoupon {
        return userCouponJpaRepository.save(userCoupon)
    }
}
