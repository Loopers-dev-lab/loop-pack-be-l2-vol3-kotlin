package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Component
class FcfsCouponService(
    private val couponRepository: CouponRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {

    @Transactional
    fun requestFcfsIssue(couponId: Long, userId: Long): FcfsCouponIssueInfo {
        val coupon = couponRepository.findByIdAndDeletedAtIsNull(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")

        if (coupon.maxIssueCount == null) {
            throw CoreException(ErrorType.BAD_REQUEST, "선착순 쿠폰이 아닙니다.")
        }

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }

        val requestId = UUID.randomUUID().toString()
        val request = couponIssueRequestRepository.save(
            CouponIssueRequest(
                requestId = requestId,
                couponId = couponId,
                userId = userId,
            ),
        )

        val message = KafkaEventMessage(
            eventId = requestId,
            eventType = "COUPON_ISSUE_REQUESTED",
            aggregateType = "COUPON",
            aggregateId = couponId.toString(),
            payload = mapOf(
                "requestId" to requestId,
                "couponId" to couponId,
                "userId" to userId,
            ),
            version = request.id,
            occurredAt = ZonedDateTime.now(),
        )
        kafkaTemplate.send(KafkaTopics.COUPON_ISSUE_REQUESTS, couponId.toString(), message)

        return FcfsCouponIssueInfo(requestId = requestId)
    }

    @Transactional(readOnly = true)
    fun getIssueStatus(requestId: String): FcfsCouponStatusInfo {
        val request = couponIssueRequestRepository.findByRequestId(requestId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다.")

        return FcfsCouponStatusInfo(
            requestId = request.requestId,
            couponId = request.couponId,
            status = request.status.name,
            failureReason = request.failureReason,
        )
    }
}

data class FcfsCouponIssueInfo(
    val requestId: String,
)

data class FcfsCouponStatusInfo(
    val requestId: String,
    val couponId: Long,
    val status: String,
    val failureReason: String?,
)
