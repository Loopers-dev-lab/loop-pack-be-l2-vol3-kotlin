package com.loopers.application.coupon

import com.loopers.application.outbox.OutboxEventWriter
import com.loopers.domain.coupon.CouponCounterStore
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.outbox.OutboxEventType
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RequestCouponIssueAsyncUseCase(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val couponCounterStore: CouponCounterStore,
    private val outboxEventWriter: OutboxEventWriter,
) {

    @Transactional
    fun execute(command: CouponCommand.IssueAsync): CouponIssueRequestInfo {
        val coupon = couponRepository.findActiveByIdOrNull(command.couponId)
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        if (coupon.isExpired()) {
            throw CoreException(CouponErrorCode.COUPON_EXPIRED)
        }

        if (userCouponRepository.existsByUserIdAndCouponId(command.userId, command.couponId)) {
            throw CoreException(CouponErrorCode.ALREADY_ISSUED_COUPON)
        }

        val maxQuantity = coupon.maxQuantity
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        // Redis INCR 게이트: 수량 초과 시 즉시 거절
        val currentCount = couponCounterStore.increment(command.couponId)
        if (currentCount > maxQuantity) {
            couponCounterStore.decrement(command.couponId)
            throw CoreException(CouponErrorCode.COUPON_SOLD_OUT)
        }

        // 요청 저장 + outbox 기록 (같은 TX)
        // INCR 후 DB 실패 시 카운터 보정을 위해 try-catch
        try {
            val issueRequest = CouponIssueRequest.create(
                couponId = command.couponId,
                userId = command.userId,
            )
            val saved = couponIssueRequestRepository.save(issueRequest)

            val event = CouponIssueRequestEvent(
                requestId = saved.requestId,
                couponId = saved.couponId,
                userId = saved.userId,
            )
            outboxEventWriter.write(
                eventType = OutboxEventType.COUPON_ISSUE_REQUESTED,
                partitionKey = command.couponId.toString(),
                payload = event,
            )

            return CouponIssueRequestInfo.from(saved)
        } catch (e: Exception) {
            couponCounterStore.decrement(command.couponId)
            throw e
        }
    }
}
