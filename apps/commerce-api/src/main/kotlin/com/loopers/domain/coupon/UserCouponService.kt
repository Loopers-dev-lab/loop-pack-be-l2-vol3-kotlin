package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCouponService(
    private val userCouponRepository: UserCouponRepository,
) {
    @Transactional
    fun issueCoupon(userId: Long, couponTemplateId: Long): UserCouponModel {
        try {
            return userCouponRepository.save(
                UserCouponModel(
                    userId = userId,
                    couponTemplateId = couponTemplateId,
                    status = UserCouponStatus.AVAILABLE,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급된 쿠폰입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getUserCouponsByUserId(userId: Long): List<UserCouponModel> {
        return userCouponRepository.findAllByUserId(userId)
    }

    @Transactional(readOnly = true)
    fun getUserCouponByIdAndUserId(id: Long, userId: Long): UserCouponModel {
        return userCouponRepository.findByIdAndUserId(id, userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getIssuesByCouponTemplateId(couponTemplateId: Long, pageable: Pageable): Page<UserCouponModel> {
        return userCouponRepository.findAllByCouponTemplateId(couponTemplateId, pageable)
    }
}
