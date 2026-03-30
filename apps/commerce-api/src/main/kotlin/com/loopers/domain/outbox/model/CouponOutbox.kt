package com.loopers.domain.outbox.model

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class CouponOutbox(
    val id: Long = 0,
    val eventId: String,
    val eventType: CouponOutboxEventType,
    val couponId: CouponId,
    val userId: UserId,
    published: Boolean = false,
) {

    var published: Boolean = published
        private set

    enum class CouponOutboxEventType {
        COUPON_ISSUE_REQUESTED,
    }

    init {
        if (eventId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventId는 필수입니다.")
        if (couponId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "couponId는 양수여야 합니다.")
        if (userId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
    }

    fun markPublished() {
        published = true
    }
}
