package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.coupon.CouponIssueRequestEntity
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.UserCouponEntity
import com.loopers.infrastructure.coupon.UserCouponJpaRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponIssueConsumer(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val userCouponJpaRepository: UserCouponJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["coupon-issue-requests"],
        groupId = "streamer-coupon-issue",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    @Transactional
    fun consume(
        record: ConsumerRecord<Any, Any>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            processRecord(record)
        } catch (e: Exception) {
            log.error("쿠폰 발급 처리 실패. offset={}, key={}", record.offset(), record.key(), e)
        }
        acknowledgment.acknowledge()
    }

    @Suppress("UNCHECKED_CAST")
    private fun processRecord(record: ConsumerRecord<Any, Any>) {
        val payload = record.value() as? Map<String, Any> ?: return
        val requestId = payload["requestId"]?.toString() ?: return
        val couponId = (payload["couponId"] as? Number)?.toLong() ?: return
        val userId = (payload["userId"] as? Number)?.toLong() ?: return

        val issueRequest = couponIssueRequestJpaRepository.findByRequestId(requestId)
        if (issueRequest == null) {
            log.warn("발급 요청을 찾을 수 없습니다. requestId={}", requestId)
            return
        }

        if (issueRequest.status != "PENDING") {
            log.info("이미 처리된 발급 요청입니다. requestId={}, status={}", requestId, issueRequest.status)
            return
        }

        val coupon = couponJpaRepository.findById(couponId).orElse(null)
        if (coupon == null) {
            updateRequestFailed(issueRequest, "쿠폰을 찾을 수 없습니다.")
            return
        }

        if (coupon.deletedAt != null) {
            updateRequestFailed(issueRequest, "삭제된 쿠폰입니다.")
            return
        }

        if (ZonedDateTime.now().isAfter(coupon.expiredAt)) {
            updateRequestFailed(issueRequest, "만료된 쿠폰입니다.")
            return
        }

        if (userCouponJpaRepository.existsByCouponIdAndUserId(couponId, userId)) {
            updateRequestFailed(issueRequest, "이미 발급받은 쿠폰입니다.")
            return
        }

        val affected = couponJpaRepository.incrementIssuedCount(couponId)
        if (affected == 0) {
            updateRequestFailed(issueRequest, "선착순 수량이 소진되었습니다.")
            return
        }

        val userCouponEntity = UserCouponEntity(
            id = null,
            couponId = couponId,
            userId = userId,
            status = "AVAILABLE",
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            expiredAt = coupon.expiredAt,
            usedAt = null,
            issuedAt = ZonedDateTime.now(),
        )
        userCouponJpaRepository.save(userCouponEntity)

        updateRequestSuccess(issueRequest)

        log.info("쿠폰 발급 성공. requestId={}, couponId={}, userId={}", requestId, couponId, userId)
    }

    private fun updateRequestFailed(request: CouponIssueRequestEntity, reason: String) {
        log.warn("쿠폰 발급 실패. requestId={}, reason={}", request.requestId, reason)
        val updated = CouponIssueRequestEntity(
            id = request.id,
            requestId = request.requestId,
            couponId = request.couponId,
            userId = request.userId,
            status = "FAILED",
            failureReason = reason,
            processedAt = ZonedDateTime.now(),
        )
        couponIssueRequestJpaRepository.save(updated)
    }

    private fun updateRequestSuccess(request: CouponIssueRequestEntity) {
        val updated = CouponIssueRequestEntity(
            id = request.id,
            requestId = request.requestId,
            couponId = request.couponId,
            userId = request.userId,
            status = "SUCCESS",
            failureReason = null,
            processedAt = ZonedDateTime.now(),
        )
        couponIssueRequestJpaRepository.save(updated)
    }
}
