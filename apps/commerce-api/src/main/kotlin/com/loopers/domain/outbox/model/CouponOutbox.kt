package com.loopers.domain.outbox.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class CouponOutbox(
    val id: Long = 0,
    val eventId: String,
    val eventType: String,
    val couponId: Long,
    val userId: Long,
    published: Boolean = false,
) {

    var published: Boolean = published
        private set

    init {
        if (eventId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventId는 필수입니다.")
        if (eventType.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventType은 필수입니다.")
        if (couponId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "couponId는 양수여야 합니다.")
        if (userId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
    }

    fun markPublished() {
        published = true
    }
}
