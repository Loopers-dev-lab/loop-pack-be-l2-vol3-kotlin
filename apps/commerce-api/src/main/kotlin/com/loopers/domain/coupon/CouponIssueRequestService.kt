package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueRequestService(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val couponIssueService: CouponIssueService,
) {
    @Transactional
    fun create(couponId: Long, userId: Long): CouponIssueRequestModel {
        return couponIssueRequestRepository.save(
            CouponIssueRequestModel(
                couponId = couponId,
                userId = userId,
                status = CouponIssueRequestStatus.ACCEPTED,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): CouponIssueRequestModel {
        return couponIssueRequestRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 발급 요청입니다: $id")
    }

    @Transactional
    fun process(requestId: Long) {
        val request = couponIssueRequestRepository.findByIdForUpdate(requestId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 발급 요청입니다: $requestId")

        if (request.isFinalStatus()) {
            return
        }

        request.markProcessing()

        when (val result = couponIssueService.issueFromRequest(request.couponId, request.userId)) {
            is CouponIssueProcessResult.Completed -> request.markCompleted(result.couponIssueId)
            CouponIssueProcessResult.Duplicate -> request.markDuplicate()
            CouponIssueProcessResult.SoldOut -> request.markSoldOut()
            CouponIssueProcessResult.Expired -> request.markExpired()
        }
    }
}
