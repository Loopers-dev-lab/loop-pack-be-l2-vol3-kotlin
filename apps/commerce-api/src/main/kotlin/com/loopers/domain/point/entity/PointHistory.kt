package com.loopers.domain.point.entity

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class PointHistory(
    refUserPointId: Long,
    type: PointHistoryType,
    amount: Long,
    refOrderId: Long? = null,
) {

    init {
        if (amount <= 0) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "포인트 이력 금액은 0보다 커야 합니다.",
            )
        }
    }

    enum class PointHistoryType {
        CHARGE,
        USE,
    }

    val id: Long = 0

    var refUserPointId: Long = refUserPointId
        protected set

    var type: PointHistoryType = type
        protected set

    var amount: Long = amount
        protected set

    var refOrderId: Long? = refOrderId
        protected set

    val createdAt: ZonedDateTime = ZonedDateTime.now()
}
