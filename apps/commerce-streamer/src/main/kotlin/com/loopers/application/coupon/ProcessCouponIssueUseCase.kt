package com.loopers.application.coupon

import com.loopers.domain.coupon.repository.CouponIssueRequestRepository
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.event.repository.EventHandledRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProcessCouponIssueUseCase(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val eventHandledRepository: EventHandledRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(eventId: String, couponId: Long, userId: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        val request = couponIssueRequestRepository.findByRequestId(eventId)
        if (request == null) {
            log.warn("발급 요청을 찾을 수 없음: requestId={}", eventId)
            return
        }

        if (issuedCouponRepository.existsByRefCouponIdAndRefUserId(couponId, userId)) {
            request.markDuplicate()
            couponIssueRequestRepository.save(request)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            log.info("중복 발급 요청: couponId={}, userId={}", couponId, userId)
            return
        }

        val coupon = couponRepository.findById(couponId)
        if (coupon == null) {
            request.markFailed()
            couponIssueRequestRepository.save(request)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            log.warn("쿠폰을 찾을 수 없음: couponId={}", couponId)
            return
        }

        if (!coupon.canIssue()) {
            if (coupon.isSoldOut()) {
                request.markSoldOut()
                log.info("쿠폰 소진: couponId={}", couponId)
            } else {
                request.markFailed()
                log.warn("쿠폰 발급 불가(삭제/만료): couponId={}", couponId)
            }
            couponIssueRequestRepository.save(request)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        coupon.issue()
        couponRepository.save(coupon)
        issuedCouponRepository.save(refCouponId = couponId, refUserId = userId)

        request.markSuccess()
        couponIssueRequestRepository.save(request)
        eventHandledRepository.save(EventHandled(eventId = eventId))
    }
}
