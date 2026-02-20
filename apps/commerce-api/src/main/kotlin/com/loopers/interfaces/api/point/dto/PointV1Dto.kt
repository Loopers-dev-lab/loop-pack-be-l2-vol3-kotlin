package com.loopers.interfaces.api.point.dto

import com.loopers.domain.point.entity.UserPoint

class PointV1Dto {
    data class BalanceResponse(
        val userId: Long,
        val balance: Long,
    ) {
        companion object {
            fun from(userPoint: UserPoint): BalanceResponse {
                return BalanceResponse(
                    userId = userPoint.refUserId,
                    balance = userPoint.balance,
                )
            }
        }
    }
}
