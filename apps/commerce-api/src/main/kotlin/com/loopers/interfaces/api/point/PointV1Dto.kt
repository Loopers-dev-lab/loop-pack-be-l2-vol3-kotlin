package com.loopers.interfaces.api.point

import com.loopers.domain.point.PointCommand
import com.loopers.domain.point.UserPoint

class PointV1Dto {
    data class ChargeRequest(
        val amount: Long,
    ) {
        fun toCommand(): PointCommand.Charge {
            return PointCommand.Charge(amount = amount)
        }
    }

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
