package com.loopers.domain.point.model

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
        private set

    var type: PointHistoryType = type
        private set

    var amount: Long = amount
        private set

    var refOrderId: Long? = refOrderId
        private set

    val createdAt: ZonedDateTime = ZonedDateTime.now()
}
