package com.loopers.application.fcfscoupon

import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestModel
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateModel
import java.time.ZonedDateTime

data class FcfsCouponTemplateInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long?,
    val maxDiscountAmount: Long?,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val status: String,
    val startedAt: ZonedDateTime?,
    val endedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(model: FcfsCouponTemplateModel): FcfsCouponTemplateInfo = FcfsCouponTemplateInfo(
            id = model.id,
            name = model.name,
            description = model.description,
            discountType = model.discountType.name,
            discountValue = model.discountValue,
            minOrderAmount = model.minOrderAmount,
            maxDiscountAmount = model.maxDiscountAmount,
            totalQuantity = model.totalQuantity,
            issuedQuantity = model.issuedQuantity,
            status = model.status.name,
            startedAt = model.startedAt,
            endedAt = model.endedAt,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
        )
    }
}

data class FcfsCouponIssueRequestInfo(
    val id: Long,
    val templateId: Long,
    val memberId: Long,
    val status: String,
    val createdAt: ZonedDateTime?,
    val processedAt: ZonedDateTime?,
) {
    companion object {
        fun from(model: FcfsCouponIssueRequestModel): FcfsCouponIssueRequestInfo =
            FcfsCouponIssueRequestInfo(
                id = model.id,
                templateId = model.templateId,
                memberId = model.memberId,
                status = model.status.name,
                createdAt = model.createdAt,
                processedAt = model.processedAt,
            )
    }
}
