package com.loopers.domain.coupon.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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

    fun isDeleted(): Boolean = deletedAt != null

    fun isExpired(): Boolean = !expiredAt.isAfter(ZonedDateTime.now())

    fun isSoldOut(): Boolean = totalQuantity != null && issuedCount >= totalQuantity

    fun canIssue(): Boolean {
        if (isDeleted()) return false
        if (isExpired()) return false
        if (isSoldOut()) return false
        return true
    }

    fun issue() {
        if (!canIssue()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰을 발급할 수 없습니다.")
        }
        issuedCount++
    }
}
