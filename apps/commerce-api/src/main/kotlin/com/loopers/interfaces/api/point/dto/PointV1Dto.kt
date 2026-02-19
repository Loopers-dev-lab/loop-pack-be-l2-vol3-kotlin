package com.loopers.interfaces.api.point.dto

import com.loopers.domain.point.entity.UserPoint
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class PointV1Dto {
    data class ChargeRequest(
        @field:Min(value = 1, message = "충전 금액은 1 이상이어야 합니다.")
        @field:Max(value = 10_000_000, message = "1회 충전 한도는 10,000,000포인트입니다.")
        val amount: Long,
    )

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
