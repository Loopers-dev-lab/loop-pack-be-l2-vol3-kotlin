package com.loopers.application.point

import com.loopers.domain.point.model.UserPoint

data class PointBalanceInfo(
    val userId: Long,
    val balance: Long,
) {
    companion object {
        fun from(userPoint: UserPoint): PointBalanceInfo = PointBalanceInfo(
            userId = userPoint.refUserId.value,
            balance = userPoint.balance.value,
        )
    }
}
