package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponIssueRepositoryImpl(
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
) : CouponIssueRepository {

    override fun save(couponIssue: CouponIssueModel): CouponIssueModel {
        return couponIssueJpaRepository.save(couponIssue)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueModel? {
        return couponIssueJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): CouponIssueModel? {
        return couponIssueJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
    }

    override fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<CouponIssueModel> {
        return couponIssueJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
    }

    override fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long, pageable: Pageable): Page<CouponIssueModel> {
        return couponIssueJpaRepository.findAllByCouponIdAndDeletedAtIsNull(couponId, pageable)
    }

    override fun findByIdForUpdate(id: Long): CouponIssueModel? {
        return couponIssueJpaRepository.findByIdForUpdate(id)
    }
}
