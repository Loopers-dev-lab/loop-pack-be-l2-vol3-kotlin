package com.loopers.domain.coupon.model

import java.time.ZonedDateTime

class Coupon(
    val id: Long = 0,
    val totalQuantity: Int?,
    issuedCount: Int = 0,
    val expiredAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {

    var issuedCount: Int = issuedCount
        private set

    fun canIssue(): Boolean {
        if (deletedAt != null) return false
        if (!expiredAt.isAfter(ZonedDateTime.now())) return false
        if (totalQuantity != null && issuedCount >= totalQuantity) return false
        return true
    }

    fun issue() {
        issuedCount++
    }
}
