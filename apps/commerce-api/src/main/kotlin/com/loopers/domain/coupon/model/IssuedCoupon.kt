package com.loopers.domain.coupon.model

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class IssuedCoupon(
    val id: Long = 0,
    val refCouponId: CouponId,
    val refUserId: UserId,
    status: CouponStatus = CouponStatus.AVAILABLE,
    usedAt: ZonedDateTime? = null,
    val createdAt: ZonedDateTime,
) {

    var status: CouponStatus = status
        private set

    var usedAt: ZonedDateTime? = usedAt
        private set

    init {
        if (status == CouponStatus.USED && usedAt == null) {
            throw CoreException(ErrorType.BAD_REQUEST, "USED 상태의 쿠폰은 usedAt이 필수입니다.")
        }
        if (status != CouponStatus.USED && usedAt != null) {
            throw CoreException(ErrorType.BAD_REQUEST, "USED 상태가 아닌 쿠폰은 usedAt이 null이어야 합니다.")
        }
    }

    enum class CouponStatus {
        AVAILABLE,
        USED,
        EXPIRED,
    }

    fun use() {
        if (status != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
        }
        status = CouponStatus.USED
        usedAt = ZonedDateTime.now()
    }

    fun isAvailable(): Boolean = status == CouponStatus.AVAILABLE

    fun isOwnedBy(userId: UserId): Boolean = refUserId == userId
}
