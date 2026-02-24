package com.loopers.domain.point.model

import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class PointHistory(
    val id: Long = 0,
    val refUserPointId: Long,
    val type: PointHistoryType,
    val amount: Point,
    val refOrderId: Long? = null,
) {

    init {
        if (amount.value <= 0) {
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
}
