package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CompensationStatus
import com.loopers.domain.coupon.CouponCompensationRepository
import com.loopers.domain.coupon.CouponService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 USED 마킹 실패 건에 대한 보상 스케줄러.
 *
 * 5분 간격으로 PENDING 상태인 보상 건을 조회하여 재시도한다.
 * - 성공 시 RESOLVED
 * - 재시도 3회 초과 시 FAILED (운영자 확인 필요)
 */
@Component
class CouponCompensationScheduler(
    private val compensationRepository: CouponCompensationRepository,
    private val couponService: CouponService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(CouponCompensationScheduler::class.java)
        private const val MAX_RETRY = 3
    }

    @Scheduled(fixedDelay = 300_000) // 5분
    @Transactional
    fun processCompensation() {
        val pendingList = compensationRepository.findAllByStatus(CompensationStatus.PENDING)
        if (pendingList.isEmpty()) return

        log.info("쿠폰 보상 처리 시작 — {}건", pendingList.size)

        pendingList.forEach { compensation ->
            try {
                couponService.markCouponUsed(compensation.couponIssueId)
                compensation.markResolved()
                log.info(
                    "쿠폰 보상 성공 [orderId={}, couponIssueId={}]",
                    compensation.orderId,
                    compensation.couponIssueId,
                )
            } catch (e: Exception) {
                compensation.incrementRetry()
                if (compensation.retryCount >= MAX_RETRY) {
                    compensation.markFailed()
                    log.error(
                        "쿠폰 보상 최종 실패 — 운영자 확인 필요 [orderId={}, couponIssueId={}]",
                        compensation.orderId,
                        compensation.couponIssueId,
                        e,
                    )
                } else {
                    log.warn(
                        "쿠폰 보상 재시도 {}/{} [orderId={}, couponIssueId={}]",
                        compensation.retryCount,
                        MAX_RETRY,
                        compensation.orderId,
                        compensation.couponIssueId,
                        e,
                    )
                }
            }
        }
    }
}
