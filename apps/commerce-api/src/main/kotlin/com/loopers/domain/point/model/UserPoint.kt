package com.loopers.domain.point.model

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class UserPoint(
    val id: Long = 0,
    val refUserId: UserId,
    balance: Point = Point(0),
) {

    var balance: Point = balance
        private set

    init {
        // Point init 블록에서 0 이상 검증
    }

    fun charge(amount: Point) {
        if (amount.value == 0L) {
            throw CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.")
        }
        val newBalance = balance.plus(amount)
        if (newBalance.value > MAX_BALANCE) {
            throw CoreException(ErrorType.BAD_REQUEST, "충전 후 잔액이 최대 한도(${MAX_BALANCE}포인트)를 초과합니다.")
        }
        this.balance = newBalance
    }

    companion object {
        const val MAX_BALANCE = 10_000_000L
    }

    fun use(amount: Point) {
        if (amount.value == 0L) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 포인트는 0보다 커야 합니다.")
        }
        if (!canAfford(amount)) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "포인트가 부족합니다. 필요: ${amount.value}, 현재: ${balance.value}",
            )
        }
        this.balance = balance.minus(amount)
    }

    fun canAfford(amount: Point): Boolean {
        return balance.isGreaterThanOrEqual(amount)
    }
}
