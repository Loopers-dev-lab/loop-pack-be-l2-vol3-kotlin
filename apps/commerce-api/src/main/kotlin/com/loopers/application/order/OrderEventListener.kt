package com.loopers.application.order

import com.loopers.config.async.AsyncConfig
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.event.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 주문 이벤트 리스너.
 *
 * AFTER_COMMIT 시점에 비동기로 실행되어 주문 트랜잭션에 영향을 주지 않는다.
 * 각 핸들러는 독립적으로 동작하며, 하나의 실패가 다른 핸들러에 영향 없음.
 */
@Component
class OrderEventListener(
    private val couponService: CouponService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventListener::class.java)
        private const val MAX_RETRY = 3
        private const val RETRY_INTERVAL_MS = 1000L
    }

    /**
     * 쿠폰 USED 마킹 — 주문 커밋 후 비동기 처리.
     *
     * - REQUIRES_NEW: 주문 트랜잭션과 별도 트랜잭션
     * - 재시도 3회 후 실패 시 로그 기록 (Step 1-7에서 compensation 테이블로 확장)
     * - couponIssueId가 null이면 쿠폰 미사용 주문이므로 스킵
     */
    @Async(AsyncConfig.EVENT_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleCouponUsage(event: OrderEvent.Created) {
        val couponIssueId = event.couponIssueId ?: return

        var lastException: Exception? = null
        repeat(MAX_RETRY) { attempt ->
            try {
                couponService.markCouponUsed(couponIssueId)
                log.info(
                    "쿠폰 USED 마킹 완료 [orderId={}, couponIssueId={}]",
                    event.aggregateId,
                    couponIssueId,
                )
                return
            } catch (e: Exception) {
                lastException = e
                log.warn(
                    "쿠폰 USED 마킹 재시도 {}/{} [orderId={}, couponIssueId={}]",
                    attempt + 1,
                    MAX_RETRY,
                    event.aggregateId,
                    couponIssueId,
                    e,
                )
                if (attempt < MAX_RETRY - 1) {
                    Thread.sleep(RETRY_INTERVAL_MS)
                }
            }
        }

        // 재시도 소진 — 보상 테이블에 기록
        try {
            couponService.saveCompensation(
                orderId = event.aggregateId,
                couponIssueId = couponIssueId,
                eventId = event.eventId,
            )
            log.error(
                "쿠폰 USED 마킹 최종 실패 — 보상 테이블에 기록됨 [orderId={}, couponIssueId={}, eventId={}]",
                event.aggregateId,
                couponIssueId,
                event.eventId,
                lastException,
            )
        } catch (e: Exception) {
            log.error(
                "쿠폰 보상 기록마저 실패 — 수동 처리 필요 [orderId={}, couponIssueId={}, eventId={}]",
                event.aggregateId,
                couponIssueId,
                event.eventId,
                e,
            )
        }
    }

    /**
     * 유저 행동 로깅 — 주문 생성 기록.
     *
     * 실패해도 주문에 영향 없음. 예외 발생 시 로그만 남긴다.
     */
    @Async(AsyncConfig.EVENT_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLogging(event: OrderEvent.Created) {
        try {
            log.info(
                "[주문 로깅] orderId={}, userId={}, orderNumber={}, totalAmount={}, items={}",
                event.aggregateId,
                event.userId,
                event.orderNumber,
                event.totalAmount,
                event.orderItems.size,
            )
        } catch (e: Exception) {
            log.warn("주문 로깅 실패 [orderId={}]", event.aggregateId, e)
        }
    }

    /**
     * 알림 시뮬레이션 — 실제 발송 없이 로그로 대체.
     *
     * 실무에서는 슬랙, 이메일, 푸시 등으로 확장 가능.
     * 실패해도 주문에 영향 없음.
     */
    @Async(AsyncConfig.EVENT_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleNotification(event: OrderEvent.Created) {
        try {
            log.info(
                "[알림 시뮬레이션] 주문 완료 알림 발송 — userId={}, orderNumber={}, totalAmount={}",
                event.userId,
                event.orderNumber,
                event.totalAmount,
            )
        } catch (e: Exception) {
            log.warn("주문 알림 발송 실패 [orderId={}]", event.aggregateId, e)
        }
    }
}
