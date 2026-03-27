package com.loopers.domain.coupon

import java.math.BigDecimal
import java.time.ZonedDateTime

data class CouponTemplateDto(
    val id: Long,
    val name: String,
    val type: CouponType,
    val value: BigDecimal,
    val minOrderAmount: BigDecimal,
    val expiredAt: ZonedDateTime,
    val totalCount: Int? = null,
    val issuedCount: Int = 0,
) {
    fun isExpired(): Boolean {
        return ZonedDateTime.now() > expiredAt
    }

    fun isAvailable(): Boolean {
        return totalCount == null || issuedCount < totalCount
    }
}
