package com.loopers.domain.coupon

import com.loopers.infrastructure.coupon.CouponCounterRedisRepository
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueService(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val couponCounterRedisRepository: CouponCounterRedisRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processIssueRequest(requestId: String, userId: Long, couponTemplateId: Long) {
        val request = couponIssueRequestJpaRepository.findByRequestId(requestId)
        if (request == null) {
            log.warn("[CouponIssue] Request not found: requestId={}", requestId)
            return
        }

        if (request.status != CouponIssueStatus.PENDING) {
            log.info("[CouponIssue] Request already processed: requestId={} status={}", requestId, request.status)
            return
        }

        if (issuedCouponJpaRepository.existsByUserIdAndCouponTemplateId(userId, couponTemplateId)) {
            request.fail("이미 발급된 쿠폰입니다.")
            log.info("[CouponIssue] Duplicate issuance prevented: userId={} templateId={}", userId, couponTemplateId)
            return
        }

        val remaining = couponCounterRedisRepository.decrementAndGet(couponTemplateId)
        if (remaining < 0) {
            couponCounterRedisRepository.increment(couponTemplateId)
            request.fail("매진")
            log.info("[CouponIssue] Sold out: templateId={}", couponTemplateId)
            return
        }

        issuedCouponJpaRepository.save(IssuedCoupon(userId = userId, couponTemplateId = couponTemplateId))
        request.complete()
        log.info("[CouponIssue] Issued successfully: userId={} templateId={} remaining={}", userId, couponTemplateId, remaining)
    }

    fun initializeCounter(templateId: Long) {
        val template = couponTemplateJpaRepository.findById(templateId).orElse(null) ?: return
        val maxCount = template.maxIssuanceCount ?: return
        val issuedCount = issuedCouponJpaRepository.findAll().count { it.couponTemplateId == templateId }
        val remaining = maxCount - issuedCount
        couponCounterRedisRepository.initCounter(templateId, remaining.coerceAtLeast(0))
        log.info("[CouponIssue] Counter initialized: templateId={} remaining={}", templateId, remaining)
    }
}
