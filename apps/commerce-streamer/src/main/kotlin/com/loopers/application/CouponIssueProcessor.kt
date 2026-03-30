package com.loopers.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.event.EventEnvelope
import com.loopers.event.payload.CouponIssueRequestPayload
import com.loopers.infrastructure.coupon.CouponIssueRedisRepository
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.event.EventHandledJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueProcessor(
    private val eventHandledRepository: EventHandledJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponIssueRedisRepository: CouponIssueRedisRepository,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(envelope: EventEnvelope) {
        // 1. 멱등성 체크 — INSERT IGNORE로 원자적 보장
        if (eventHandledRepository.insertIgnore(envelope.eventId) == 0) {
            log.debug("[CouponIssue] 이미 처리된 이벤트 스킵: eventId={}", envelope.eventId)
            return
        }

        val payload = try {
            objectMapper.readValue(envelope.payload, CouponIssueRequestPayload::class.java)
        } catch (e: Exception) {
            log.error("[CouponIssue] 페이로드 파싱 실패: eventId={}, payload={}", envelope.eventId, envelope.payload, e)
            return
        }

        val issueRequest = couponIssueRequestJpaRepository.findByRequestId(payload.requestId)
        if (issueRequest == null) {
            log.error("[CouponIssue] 발급 요청을 찾을 수 없습니다: eventId={}, requestId={}", envelope.eventId, payload.requestId)
            return
        }

        try {
            if (issuedCouponJpaRepository.existsByCouponIdAndUserId(payload.couponId, payload.userId)) {
                throw IllegalStateException("이미 발급된 쿠폰입니다.")
            }

            val coupon = couponJpaRepository.findById(payload.couponId)
                .orElseThrow { IllegalStateException("쿠폰을 찾을 수 없습니다. couponId=${payload.couponId}") }
            coupon.issue()

            issuedCouponJpaRepository.save(IssuedCoupon(couponId = payload.couponId, userId = payload.userId))
            issueRequest.markIssued()
        } catch (e: Exception) {
            log.warn("[CouponIssue] 발급 실패: eventId={}, reason={}", envelope.eventId, e.message)
            issueRequest.markFailed(e.message ?: "알 수 없는 오류")
            couponIssueRedisRepository.restore(payload.couponId, payload.userId)
        }
    }
}
