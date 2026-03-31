package com.loopers.domain.fcfscoupon

import java.time.ZonedDateTime

data class FcfsCouponIssueRequestModel(
    val id: Long = 0,
    val templateId: Long,
    val memberId: Long,
    val status: FcfsCouponIssueStatus = FcfsCouponIssueStatus.PENDING,
    val createdAt: ZonedDateTime? = null,
    val processedAt: ZonedDateTime? = null,
)
