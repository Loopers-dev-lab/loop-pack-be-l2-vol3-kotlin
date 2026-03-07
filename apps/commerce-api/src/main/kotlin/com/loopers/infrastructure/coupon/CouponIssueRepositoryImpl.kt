package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssue
import com.loopers.domain.coupon.CouponIssueRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CouponIssueRepositoryImpl(
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
) : CouponIssueRepository {

    override fun save(couponIssue: CouponIssue): CouponIssue {
        return couponIssueJpaRepository.save(couponIssue)
    }

    override fun findById(id: Long): CouponIssue? {
        return couponIssueJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): CouponIssue? {
        return couponIssueJpaRepository.findByUserIdAndCouponId(userId, couponId)
    }

    override fun findAllByUserId(userId: Long): List<CouponIssue> {
        return couponIssueJpaRepository.findAllByUserId(userId)
    }

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssue> {
        return couponIssueJpaRepository.findAllByCouponId(couponId, pageable)
    }
}
