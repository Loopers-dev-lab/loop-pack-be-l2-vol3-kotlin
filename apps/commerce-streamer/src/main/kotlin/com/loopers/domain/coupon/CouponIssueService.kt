package com.loopers.domain.coupon

import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponIssueService(
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processIssue(couponId: Long, userId: Long) {
        val request = couponIssueRequestJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
        if (request == null) {
            log.warn("발급 요청을 찾을 수 없음 - couponId: {}, userId: {}", couponId, userId)
            return
        }

        if (request.status != CouponIssueRequestStatus.PENDING) {
            log.info("이미 처리된 요청 - couponId: {}, userId: {}, status: {}", couponId, userId, request.status)
            return
        }

        try {
            val existingIssue = issuedCouponJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
            if (existingIssue != null) {
                request.markFailed("이미 발급된 쿠폰입니다.")
                return
            }

            val coupon = couponJpaRepository.findByIdAndDeletedAtIsNull(couponId)
                ?: run {
                    request.markFailed("쿠폰을 찾을 수 없습니다.")
                    return
                }

            coupon.issue()

            issuedCouponJpaRepository.save(
                IssuedCouponModel(
                    couponId = couponId,
                    userId = userId,
                    discountType = coupon.discountType,
                    discountValue = coupon.discountValue,
                    expiredAt = coupon.expiredAt,
                ),
            )

            request.markSuccess()
            log.info("쿠폰 발급 완료 - couponId: {}, userId: {}", couponId, userId)
        } catch (e: Exception) {
            request.markFailed(e.message ?: "알 수 없는 오류")
            log.error("쿠폰 발급 실패 - couponId: {}, userId: {}", couponId, userId, e)
        }
    }
}
