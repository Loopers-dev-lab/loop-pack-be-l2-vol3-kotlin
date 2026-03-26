package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class RequestCouponIssueAsyncUseCase(
    private val couponRepository: CouponRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {
    companion object {
        const val TOPIC_COUPON_ISSUE_REQUESTS = "coupon-issue-requests"
    }

    @Transactional
    fun request(userId: Long, couponId: Long): String {
        val coupon = couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $couponId")

        if (coupon.isDeleted()) {
            throw CoreException(ErrorType.BAD_REQUEST, "삭제된 쿠폰입니다.")
        }

        val requestId = UUID.randomUUID().toString()
        val request = CouponIssueRequest.create(
            requestId = requestId,
            couponId = couponId,
            userId = userId,
        )
        couponIssueRequestRepository.save(request)

        kafkaTemplate.send(
            TOPIC_COUPON_ISSUE_REQUESTS,
            couponId.toString(),
            mapOf(
                "requestId" to requestId,
                "couponId" to couponId,
                "userId" to userId,
            ),
        )

        return requestId
    }
}
