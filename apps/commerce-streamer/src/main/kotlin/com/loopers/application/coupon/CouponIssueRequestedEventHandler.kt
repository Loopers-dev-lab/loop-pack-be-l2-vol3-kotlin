package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.infrastructure.outbox.CouponIssueRequestedOutboxMessagePayload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponIssueRequestedEventHandler(
    private val couponRepository: CouponRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    @Transactional
    fun handle(message: CouponIssueRequestedOutboxMessagePayload) {
        val request = couponIssueRequestRepository.findByIdForUpdate(message.requestId)
            ?: throw IllegalStateException("coupon issue request not found")

        if (request.status == com.loopers.domain.coupon.CouponIssueRequest.Status.ISSUED ||
            request.status == com.loopers.domain.coupon.CouponIssueRequest.Status.FAILED
        ) {
            return
        }

        val coupon = couponRepository.findByIdForUpdate(message.couponId)
            ?: run {
                couponIssueRequestRepository.save(request.markFailed("COUPON_NOT_FOUND"))
                return
            }

        val existingIssuedCoupon = issuedCouponRepository.findByCouponIdAndUserId(message.couponId, message.userId)
        if (existingIssuedCoupon != null) {
            couponIssueRequestRepository.save(request.markIssued(existingIssuedCoupon.id!!))
            return
        }

        if (coupon.isDeleted()) {
            couponIssueRequestRepository.save(request.markFailed("COUPON_DELETED"))
            return
        }

        if (coupon.isExpired()) {
            couponIssueRequestRepository.save(request.markFailed("COUPON_EXPIRED"))
            return
        }

        if (coupon.isSoldOut()) {
            couponIssueRequestRepository.save(request.markFailed("COUPON_SOLD_OUT"))
            return
        }

        val issuedCoupon = issuedCouponRepository.save(
            IssuedCoupon.issue(
                couponId = message.couponId,
                userId = message.userId,
                expiredAt = coupon.expiredAt,
            ),
        )
        val issuedCouponId = issuedCoupon.id ?: throw IllegalStateException("issued coupon was not persisted")
        val updatedCoupon = coupon.issue()
        couponRepository.save(updatedCoupon)
        couponIssueRequestRepository.save(request.markIssued(issuedCouponId))
    }
}
