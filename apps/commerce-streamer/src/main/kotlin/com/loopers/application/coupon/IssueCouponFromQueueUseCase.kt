package com.loopers.application.coupon

import com.loopers.application.event.IdempotencyService
import com.loopers.domain.coupon.CouponIssueRepository
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponIssueRequestStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class IssueCouponFromQueueUseCase(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val couponIssueRepository: CouponIssueRepository,
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(requestId: String, eventType: String, couponId: Long, userId: Long) {
        if (!idempotencyService.tryMarkHandled(requestId, eventType)) {
            log.debug("이미 처리된 쿠폰 발급 요청 skip. requestId={}", requestId)
            return
        }

        val issueRequest = couponIssueRequestRepository.findByRequestId(requestId)
        if (issueRequest == null) {
            log.warn("발급 요청을 찾을 수 없습니다. requestId={}", requestId)
            return
        }

        if (issueRequest.status != CouponIssueRequestStatus.PENDING) {
            log.info("이미 처리된 발급 요청입니다. requestId={}, status={}", requestId, issueRequest.status)
            return
        }

        val coupon = couponIssueRepository.findCouponById(couponId)
        if (coupon == null) {
            couponIssueRequestRepository.markFailed(issueRequest.id, "쿠폰을 찾을 수 없습니다.")
            return
        }

        if (coupon.isDeleted) {
            couponIssueRequestRepository.markFailed(issueRequest.id, "삭제된 쿠폰입니다.")
            return
        }

        if (ZonedDateTime.now().isAfter(coupon.expiredAt)) {
            couponIssueRequestRepository.markFailed(issueRequest.id, "만료된 쿠폰입니다.")
            return
        }

        if (couponIssueRepository.existsUserCoupon(couponId, userId)) {
            couponIssueRequestRepository.markFailed(issueRequest.id, "이미 발급받은 쿠폰입니다.")
            return
        }

        val affected = couponIssueRepository.incrementIssuedCount(couponId)
        if (affected == 0) {
            couponIssueRequestRepository.markFailed(issueRequest.id, "선착순 수량이 소진되었습니다.")
            return
        }

        couponIssueRepository.saveUserCoupon(
            couponId = couponId,
            userId = userId,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            expiredAt = coupon.expiredAt,
        )

        couponIssueRequestRepository.markSuccess(issueRequest.id)

        log.info("쿠폰 발급 성공. requestId={}, couponId={}, userId={}", requestId, couponId, userId)
    }
}
