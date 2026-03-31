package com.loopers.interfaces.api.admin.fcfscoupon

import com.loopers.application.fcfscoupon.FcfsCouponTemplateInfo
import java.time.ZonedDateTime

class AdminFcfsCouponV1Dto {
    data class CreateRequest(
        val name: String,
        val description: String?,
        val discountType: String,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val totalQuantity: Int,
        val startedAt: ZonedDateTime,
        val endedAt: ZonedDateTime,
    )

    data class UpdateRequest(
        val name: String,
        val description: String?,
        val discountType: String,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val totalQuantity: Int,
        val startedAt: ZonedDateTime,
        val endedAt: ZonedDateTime,
    )

    data class TemplateResponse(
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
            fun from(info: FcfsCouponTemplateInfo): TemplateResponse = TemplateResponse(
                id = info.id,
                name = info.name,
                description = info.description,
                discountType = info.discountType,
                discountValue = info.discountValue,
                minOrderAmount = info.minOrderAmount,
                maxDiscountAmount = info.maxDiscountAmount,
                totalQuantity = info.totalQuantity,
                issuedQuantity = info.issuedQuantity,
                status = info.status,
                startedAt = info.startedAt,
                endedAt = info.endedAt,
                createdAt = info.createdAt,
                updatedAt = info.updatedAt,
            )
        }
    }
}
