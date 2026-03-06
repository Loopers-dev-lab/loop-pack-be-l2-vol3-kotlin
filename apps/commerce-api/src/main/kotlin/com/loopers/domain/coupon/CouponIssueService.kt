package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueService(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun issue(couponId: Long, userId: Long): CouponIssueModel {
        val coupon = couponRepository.findByIdAndDeletedAtIsNull(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다: $couponId")

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰은 발급할 수 없습니다.")
        }

        val existingIssue = couponIssueRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
        if (existingIssue != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }

        val couponIssue = CouponIssueModel(couponId = couponId, userId = userId)
        try {
            return couponIssueRepository.save(couponIssue)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): CouponIssueModel {
        return couponIssueRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 발급 쿠폰입니다: $id")
    }

    @Transactional
    fun findByIdForUpdate(id: Long): CouponIssueModel {
        return couponIssueRepository.findByIdForUpdate(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 발급 쿠폰입니다: $id")
    }

    @Transactional(readOnly = true)
    fun findByUserId(userId: Long, pageable: Pageable): Page<CouponIssueModel> {
        return couponIssueRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
    }

    @Transactional(readOnly = true)
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssueModel> {
        return couponIssueRepository.findAllByCouponIdAndDeletedAtIsNull(couponId, pageable)
    }
}
