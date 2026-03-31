package com.loopers.domain.fcfscoupon

import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

data class FcfsCouponTemplateModel(
    val id: Long = 0,
    val name: String,
    val description: String?,
    val discountType: CouponType,
    val discountValue: Long,
    val minOrderAmount: Long?,
    val maxDiscountAmount: Long?,
    val totalQuantity: Int,
    val issuedQuantity: Int = 0,
    val status: FcfsCouponTemplateStatus = FcfsCouponTemplateStatus.ACTIVE,
    val startedAt: ZonedDateTime,
    val endedAt: ZonedDateTime,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    fun isActive(): Boolean = status == FcfsCouponTemplateStatus.ACTIVE

    fun isWithinPeriod(now: ZonedDateTime = ZonedDateTime.now()): Boolean =
        now.isAfter(startedAt) && now.isBefore(endedAt)

    fun hasStock(): Boolean = issuedQuantity < totalQuantity

    fun delete(): FcfsCouponTemplateModel = copy(
        status = FcfsCouponTemplateStatus.DELETED,
        deletedAt = ZonedDateTime.now(),
    )

    fun update(
        name: String,
        description: String?,
        discountType: CouponType,
        discountValue: Long,
        minOrderAmount: Long?,
        maxDiscountAmount: Long?,
        totalQuantity: Int,
        startedAt: ZonedDateTime,
        endedAt: ZonedDateTime,
    ): FcfsCouponTemplateModel = copy(
        name = name,
        description = description,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount,
        totalQuantity = totalQuantity,
        startedAt = startedAt,
        endedAt = endedAt,
    )
}
