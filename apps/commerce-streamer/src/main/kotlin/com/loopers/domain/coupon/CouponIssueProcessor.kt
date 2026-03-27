package com.loopers.domain.coupon

import com.loopers.infrastructure.coupon.CouponIssueJpaRepository
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponQuantityJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 선착순 쿠폰 발급 처리 서비스.
 *
 * CouponIssueConsumer에서 호출되며, 트랜잭션 경계를 담당한다.
 * 1. 수량 체크 (atomic UPDATE → 락 불필요, 카프카 순차 처리와 조합)
 * 2. 중복 발급 체크 (UK: user_id + coupon_id)
 * 3. 실제 발급 (coupon_issues INSERT)
 * 4. 요청 상태 업데이트 (coupon_issue_request UPDATE)
 */
@Component
class CouponIssueProcessor(
    private val couponQuantityJpaRepository: CouponQuantityJpaRepository,
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
    private val issueRequestJpaRepository: CouponIssueRequestJpaRepository,
) {

    companion object {
        private val log = LoggerFactory.getLogger(CouponIssueProcessor::class.java)
    }

    @Transactional
    fun process(requestId: String, userId: Long, couponId: Long) {
        // 1. 수량 체크 (atomic UPDATE) — 먼저 실행하여 clearAutomatically 영향 최소화
        val acquired = couponQuantityJpaRepository.incrementIssuedCount(couponId)

        // 2. 요청 조회 — incrementIssuedCount의 clearAutomatically 이후이므로 fresh load
        val request = issueRequestJpaRepository.findByRequestId(requestId) ?: run {
            log.warn("발급 요청을 찾을 수 없음 [requestId={}]", requestId)
            return
        }

        try {
            if (acquired == 0) {
                request.markExhausted()
                log.info("[쿠폰 발급] 수량 소진 [requestId={}, couponId={}]", requestId, couponId)
                return
            }

            // 2. 중복 발급 체크
            val existing = couponIssueJpaRepository.findByUserIdAndCouponId(userId, couponId)
            if (existing != null) {
                request.markFailed("이미 발급된 쿠폰")
                log.info("[쿠폰 발급] 중복 발급 [requestId={}, userId={}, couponId={}]", requestId, userId, couponId)
                return
            }

            // 3. 실제 발급
            couponIssueJpaRepository.save(CouponIssue(couponId = couponId, userId = userId))
            request.markSuccess()
            log.info("[쿠폰 발급] 성공 [requestId={}, userId={}, couponId={}]", requestId, userId, couponId)
        } catch (e: Exception) {
            request.markFailed(e.message ?: "알 수 없는 오류")
            log.error("[쿠폰 발급] 실패 [requestId={}]", requestId, e)
        }
    }
}
