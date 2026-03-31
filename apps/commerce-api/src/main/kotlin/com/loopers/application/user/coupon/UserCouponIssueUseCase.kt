package com.loopers.application.user.coupon

import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.support.event.user.CouponIssueRequestedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@Service
class UserCouponIssueUseCase(
    private val eventPublisher: ApplicationEventPublisher,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {
    @Transactional
    fun issue(command: UserCouponCommand.Issue): UserCouponResult.IssueRequest {
        couponIssueRequestRepository.findByCouponIdAndUserId(command.couponId, command.userId)
            ?.let { return UserCouponResult.IssueRequest.from(it) }

        val request = CouponIssueRequest.request(
            couponId = command.couponId,
            userId = command.userId,
        )
        val saved = runCatching {
            couponIssueRequestRepository.save(request)
        }.getOrElse { exception ->
            if (exception is DataIntegrityViolationException) {
                couponIssueRequestRepository.findByCouponIdAndUserId(command.couponId, command.userId)
                    ?: throw exception
            } else {
                throw exception
            }
        }

        eventPublisher.publishEvent(
            CouponIssueRequestedEvent(
                requestId = saved.id!!,
                couponId = saved.couponId,
                userId = saved.userId,
            ),
        )
        return UserCouponResult.IssueRequest.from(saved)
    }
}
