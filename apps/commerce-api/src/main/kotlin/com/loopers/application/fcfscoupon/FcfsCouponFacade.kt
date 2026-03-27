package com.loopers.application.fcfscoupon

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FcfsCouponFacade(
    private val fcfsCouponService: FcfsCouponService,
    private val fcfsCouponIssueRequestPublisher: FcfsCouponIssueRequestPublisher,
) {
    @Transactional
    fun requestIssue(memberId: Long, templateId: Long): FcfsCouponIssueRequestInfo {
        val template = fcfsCouponService.getTemplate(templateId)

        if (!template.isActive()) {
            throw CoreException(ErrorType.BAD_REQUEST, "비활성 상태의 쿠폰 템플릿입니다.")
        }
        if (!template.isWithinPeriod()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급 기간이 아닙니다.")
        }
        if (!template.hasStock()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰이 모두 소진되었습니다.")
        }

        val request = fcfsCouponService.createIssueRequest(templateId, memberId)

        fcfsCouponIssueRequestPublisher.publish(
            requestId = request.id,
            templateId = templateId,
            memberId = memberId,
        )

        return FcfsCouponIssueRequestInfo.from(request)
    }

    @Transactional(readOnly = true)
    fun getIssueRequestStatus(requestId: Long, memberId: Long): FcfsCouponIssueRequestInfo {
        val request = fcfsCouponService.getIssueRequest(requestId)
        if (request.memberId != memberId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 발급 요청만 조회할 수 있습니다.")
        }
        return FcfsCouponIssueRequestInfo.from(request)
    }
}
