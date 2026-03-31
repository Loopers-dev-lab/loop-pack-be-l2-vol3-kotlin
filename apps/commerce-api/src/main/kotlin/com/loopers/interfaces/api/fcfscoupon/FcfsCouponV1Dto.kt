package com.loopers.interfaces.api.fcfscoupon

import com.loopers.application.fcfscoupon.FcfsCouponIssueRequestInfo
import java.time.ZonedDateTime

class FcfsCouponV1Dto {
    data class IssueRequestResponse(
        val requestId: Long,
        val templateId: Long,
        val status: String,
        val createdAt: ZonedDateTime?,
        val processedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: FcfsCouponIssueRequestInfo): IssueRequestResponse = IssueRequestResponse(
                requestId = info.id,
                templateId = info.templateId,
                status = info.status,
                createdAt = info.createdAt,
                processedAt = info.processedAt,
            )
        }
    }
}
