package com.loopers.interfaces.api.point.dto

import com.loopers.application.point.PointBalanceInfo

class PointV1Dto {
    data class BalanceResponse(
        val userId: Long,
        val balance: Long,
    ) {
        companion object {
            fun from(info: PointBalanceInfo): BalanceResponse {
                return BalanceResponse(
                    userId = info.userId,
                    balance = info.balance,
                )
            }
        }
    }
}
