package com.loopers.domain.coupon

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import java.time.ZonedDateTime

data class CouponTemplateModel(
    val id: Long = 0,
    val name: String,
    val type: CouponType,
    val value: Long,
    val minOrderAmount: Long?,
    val maxDiscountAmount: Long?,
    val expirationPolicy: ExpirationPolicy,
    val expiredAt: ZonedDateTime?,
    val validDays: Int?,
    val status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    fun calculateDiscount(orderAmount: Long): Long {
        val raw = when (type) {
            CouponType.FIXED -> value
            CouponType.RATE -> {
                val discount = orderAmount * value / 100
                if (maxDiscountAmount != null) minOf(discount, maxDiscountAmount) else discount
            }
        }
        return minOf(raw, orderAmount)
    }

    fun validateMinOrderAmount(orderAmount: Long) {
        if (minOrderAmount != null && orderAmount < minOrderAmount) {
            throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액 ${minOrderAmount}원 이상이어야 합니다.")
        }
    }

    fun calculateExpiredAt(issuedAt: ZonedDateTime): ZonedDateTime {
        return when (expirationPolicy) {
            ExpirationPolicy.FIXED_DATE -> expiredAt!!
            ExpirationPolicy.DAYS_FROM_ISSUE -> issuedAt.plusDays(validDays!!.toLong())
        }
    }

    fun isExpired(): Boolean {
        return when (expirationPolicy) {
            ExpirationPolicy.FIXED_DATE -> expiredAt!!.isBefore(ZonedDateTime.now())
            ExpirationPolicy.DAYS_FROM_ISSUE -> false
        }
    }

    fun isIssuable(): Boolean {
        if (isDeleted()) return false
        if (expirationPolicy == ExpirationPolicy.FIXED_DATE && isExpired()) return false
        return true
    }

    fun update(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        maxDiscountAmount: Long?,
        expirationPolicy: ExpirationPolicy,
        expiredAt: ZonedDateTime?,
        validDays: Int?,
    ): CouponTemplateModel = copy(
        name = name,
        type = type,
        value = value,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount,
        expirationPolicy = expirationPolicy,
        expiredAt = expiredAt,
        validDays = validDays,
    )

    fun delete(): CouponTemplateModel =
        copy(status = CouponTemplateStatus.DELETED, deletedAt = deletedAt ?: ZonedDateTime.now())

    fun isDeleted(): Boolean = status == CouponTemplateStatus.DELETED
}
